from fastapi import FastAPI, HTTPException
import httpx  # Asynchronous HTTP client

app = FastAPI()

API_BASE_URL = "https://world.openfoodfacts.org/api/v0/product"

async def fetch_product(barcode: str):
    """Fetch product details from Open Food Facts API asynchronously."""
    url = f"{API_BASE_URL}/{barcode}.json"
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(url, timeout=5)  # Timeout prevents hanging requests
            response.raise_for_status()  # Raise error if response status is not 2xx

        data = response.json()
        
        if data.get("status") == 1:  # Check if product exists
            return {
                "barcode": barcode,
                "product_name": data["product"].get("product_name", "Unknown"),
                "ingredients": data["product"].get("ingredients_text", "N/A"),
                "image_url": data["product"].get("image_front_url", ""),  # FIXED key
                "brands": data["product"].get("brands", "Unknown"),
                "nutritional_info": data["product"].get("nutriments", {}),
            }
    except httpx.TimeoutException:
        raise HTTPException(status_code=504, detail="Request timed out. Try again later.")
    except httpx.RequestError as e:
        raise HTTPException(status_code=503, detail=f"Error fetching product: {str(e)}")

    return None  # Return None if product not found

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
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
