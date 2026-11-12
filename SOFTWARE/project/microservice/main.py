
from fastapi import FastAPI

from duplicate_detection_service.app.routes import router as duplicate_detection_router

app = FastAPI(title="MyBugspace Microservices")
app.include_router(duplicate_detection_router, prefix="/micro/duplicate-detection")

# An endpoint to endsure FastAPI microservice is running, 
# Useful to help check remote set-up is running correctly.
@app.get("/")
def temporary():
    return {"message": "FastAPI microservice is running."}