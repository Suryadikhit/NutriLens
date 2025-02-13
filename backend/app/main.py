import os
import logging
from fastapi import FastAPI, HTTPException
import httpx
import uvicorn
from functools import lru_cache
from urllib.parse import quote

app = FastAPI()

API_BASE_URL = "https://world.openfoodfacts.org/api/v0/product"
SEARCH_BASE_URL = "https://world.openfoodfacts.org/cgi/search.pl?search_terms="
TIMEOUT = 5
RETRIES = 3

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@app.get("/products/search/{query}")
async def search_products(query: str):
    """Search products by name."""
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
                    "name": p.get("product_name", "Unknown"),
                    "imageUrl": p.get("image_front_url", "")
                } for p in products
            ]
    except httpx.RequestError as e:
        logger.error(f"Request failed: {e}")
        raise HTTPException(status_code=500, detail="Failed to fetch products from API")


@lru_cache(maxsize=100)
async def fetch_product(barcode: str):
    """Fetch product details by barcode with caching."""
    url = f"{API_BASE_URL}/{quote(barcode)}.json"
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            response = await client.get(url)
            response.raise_for_status()
            data = response.json()
            if data.get("status") == 1:
                return {
                    "barcode": barcode,
                    "name": data["product"].get("product_name", "Unknown"),
                    "ingredients": data["product"].get("ingredients_text", "N/A"),
                    "imageUrl": data["product"].get("image_front_url", ""),
                    "brands": data["product"].get("brands", "Unknown"),
                    "nutritionalInfo": data["product"].get("nutriments", {})
                }
            else:
                raise HTTPException(status_code=404, detail="Product not found")
    except httpx.RequestError as e:
        logger.error(f"Error fetching product {barcode}: {e}")
        raise HTTPException(status_code=500, detail="Error fetching product data")


@app.get("/product/{barcode}")
async def get_product(barcode: str):
    """Get product details by barcode."""
    if not barcode.isdigit():
        raise HTTPException(status_code=400, detail="Invalid barcode")
    data = await fetch_product(barcode)
    if data:
        return data
    raise HTTPException(status_code=404, detail="Product not found")


@app.get("/")
def root():
    return {"message": "Open Food Facts API Wrapper running"}

if __name__ == "__main__":
    port = int(os.getenv("PORT", 10000))
    uvicorn.run(app, host="0.0.0.0", port=port)
