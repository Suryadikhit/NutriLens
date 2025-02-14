import os
import logging
from fastapi import FastAPI, HTTPException
import httpx
import uvicorn
from urllib.parse import quote
from aiocache import cached
import asyncio

app = FastAPI()

API_BASE_URL = "https://world.openfoodfacts.org/api/v0/product"
SEARCH_BASE_URL = "https://world.openfoodfacts.org/cgi/search.pl?search_terms="
TIMEOUT = 5
RETRIES = 3

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@app.get("/products/search/{query}")
@cached(ttl=600)  # Cache search results for 10 minutes
async def search_products(query: str):
    search_url = f"{SEARCH_BASE_URL}{quote(query)}&search_simple=1&json=1"
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            response = await client.get(search_url)
            response.raise_for_status()
            data = response.json()
            products = data.get("products", [])
            if not products:
                raise HTTPException(status_code=404, detail="No products found")
            return [
                {
                    "barcode": p.get("code", ""),
                    "product_name": p.get("product_name", "Unknown"),
                    "image_url": p.get("image_front_url", "")
                } for p in products
            ]
    except httpx.RequestError as e:
        logger.error(f"Request failed: {e}")
        raise HTTPException(status_code=500, detail="Failed to fetch products from API")


@cached(ttl=600)  # Cache results for 10 minutes
async def fetch_product(barcode: str):
    url = f"{API_BASE_URL}/{quote(barcode)}.json"
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            for attempt in range(RETRIES):
                try:
                    response = await client.get(url)
                    response.raise_for_status()
                    data = response.json()
                    if data.get("status") == 1:
                        product = data["product"]

                        additives = product.get("additives_tags", [])
                        additives_list = [
                            product.get("additives_prev_tags", {}).get(a, a) for a in additives
                        ] if additives else ["No additives"]

                        carbon_footprint = product.get("carbon_footprint_data") or \
                                           product.get("nutriments", {}).get("carbon-footprint-from-known-ingredients_100g")

                        packaging = product.get("packaging_text") or product.get("packaging") or "Not specified"

                        # Extract ingredients with percentages
                        ingredients_list = []
                        if "ingredients" in product:
                            for ing in product.get("ingredients", []):
                                name = ing.get("text", "Unknown ingredient")
                                # Ensure percent_estimate is extracted correctly
                                percent = ing.get("percent_estimate", None)
                                if percent is None:
                                    percent = 0.0
                                ingredients_list.append({"name": name, "percentage": percent})

                        return {
                            "barcode": barcode,
                            "product_name": product.get("product_name") or None,
                            "quantity": product.get("quantity") or None,
                            "ingredients": ingredients_list,
                            "image_url": product.get("image_front_url") or None,
                            "brands": product.get("brands") or None,
                            "nutri_score": (product.get("nutrition_grades_tags") or [None])[0],
                            "nova_score": product.get("nova_group") or None,
                            "additives": additives_list,
                            "packaging": packaging,
                            "carbon_footprint": carbon_footprint or None,
                            "nutritional_info": product.get("nutriments", {})
                        }
                except httpx.RequestError as e:
                    logger.warning(f"Attempt {attempt+1}/{RETRIES} failed: {e}")
                    await asyncio.sleep(1)
        logger.error(f"Product {barcode} not found.")
        raise HTTPException(status_code=404, detail="Product not found")
    except Exception as e:
        logger.exception("Unexpected error occurred")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@app.get("/product/{query}")
async def get_product(query: str):
    if query.isdigit():
        data = await fetch_product(query)
        if data:
            return data
        raise HTTPException(status_code=404, detail="Product not found")
    else:
        return await search_products(query)


@app.get("/")
def root():
    return {"message": "Open Food Facts API Wrapper running"}


if __name__ == "__main__":
    port = int(os.getenv("PORT", 10000))
    uvicorn.run(app, host="0.0.0.0", port=port)
