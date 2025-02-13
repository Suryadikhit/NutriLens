import os
import logging
from fastapi import FastAPI, HTTPException
import httpx  # Asynchronous HTTP client
import uvicorn
from urllib.parse import quote

app = FastAPI()

API_BASE_URL = "https://world.openfoodfacts.org/api/v0/product"
TIMEOUT = 5  # Request timeout in seconds
RETRIES = 3  # Number of retries for API requests

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@app.get("/products/search/{query}")
async def search_products(query: str):
    """Search products by name using Open Food Facts API."""
    search_url = f"https://world.openfoodfacts.org/cgi/search.pl?search_terms={query}&search_simple=1&json=1"

    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            response = await client.get(search_url)
            response.raise_for_status()
            data = response.json()

            products = data.get("products", [])
            result = []
            for product in products:
                result.append({
                    "barcode": product.get("code", ""),
                    "product_name": product.get("product_name", "Unknown"),
                    "ingredients": product.get("ingredients_text", "N/A"),
                    "image_url": product.get("image_front_url", ""),
                    "brands": product.get("brands", "Unknown"),
                    "nutritional_info": product.get("nutriments", {})
                })

            if result:
                return result
            else:
                raise HTTPException(status_code=404, detail="No products found")

    except httpx.RequestError as e:
        logger.error(f"Request failed: {e}")
        raise HTTPException(status_code=500, detail="Failed to fetch products from API")


async def fetch_product(barcode: str):
    """Fetch product details from Open Food Facts API asynchronously."""
    url = f"{API_BASE_URL}/{quote(barcode)}.json"  # Encode barcode properly

    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            for attempt in range(RETRIES):
                try:
                    response = await client.get(url)
                    response.raise_for_status()
                    data = response.json()

                    if data.get("status") == 1:  # Product exists
                        return {
                            "barcode": barcode,
                            "product_name": data["product"].get("product_name", "Unknown"),
                            "ingredients": data["product"].get("ingredients_text", "N/A"),
                            "image_url": data["product"].get("image_front_url", ""),
                            "brands": data["product"].get("brands", "Unknown"),
                            "nutritional_info": data["product"].get("nutriments", {}),
                        }

                except httpx.RequestError as e:
                    logger.warning(f"Attempt {attempt+1}/{RETRIES} failed: {e}")

        logger.error(f"Product {barcode} not found.")
        return None

    except KeyError as e:
        logger.error(f"Unexpected data format: {e}")
        raise HTTPException(status_code=500, detail="Unexpected API response format.")

    except httpx.TimeoutException:
        logger.error("API request timed out")
        raise HTTPException(status_code=504, detail="Request timed out. Try again later.")

    except Exception as e:
        logger.exception("Unexpected error occurred")
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@app.get("/")
def root():
    """Root route to confirm API is running."""
    return {"message": "Welcome to the Open Food Facts API Wrapper!"}


@app.get("/product/{barcode}")
async def get_product(barcode: str):
    """Get product details by barcode."""
    data = await fetch_product(barcode)

    if data:
        return data

    raise HTTPException(status_code=404, detail="Product not found")


if __name__ == "__main__":
    port = int(os.getenv("PORT", 10000))  # Render provides PORT dynamically
    uvicorn.run(app, host="0.0.0.0", port=port)
