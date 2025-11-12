from fastapi import FastAPI, HTTPException, Depends, status
from pydantic import BaseModel, EmailStr
from motor.motor_asyncio import AsyncIOMotorClient
from passlib.context import CryptContext
from jose import jwt, JWTError
from datetime import datetime, timedelta
import os
from dotenv import load_dotenv

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")
JWT_SECRET = os.getenv("JWT_SECRET")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "1440"))

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

app = FastAPI()

client = AsyncIOMotorClient(MONGO_URI)
db = client.get_default_database()  # usará el DB indicado en URI

users_coll = db["users"]

class UserIn(BaseModel):
    email: EmailStr
    password: str
    name: str | None = None

class UserOut(BaseModel):
    id: str
    email: EmailStr
    name: str | None = None

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)

def create_access_token(data: dict, expires_delta: timedelta | None = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    token = jwt.encode(to_encode, JWT_SECRET, algorithm=JWT_ALGORITHM)
    return token

async def get_user_by_email(email: str):
    return await users_coll.find_one({"email": email})

@app.post("/register", response_model=UserOut)
async def register(user: UserIn):
    existing = await get_user_by_email(user.email)
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")
    hashed = hash_password(user.password)
    doc = {"email": user.email, "password": hashed, "name": user.name}
    res = await users_coll.insert_one(doc)
    return {"id": str(res.inserted_id), "email": user.email, "name": user.name}

@app.post("/login", response_model=Token)
async def login(form: UserIn):
    user = await get_user_by_email(form.email)
    if not user or not verify_password(form.password, user["password"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    token = create_access_token({"sub": user["email"], "id": str(user["_id"])})
    return {"access_token": token, "token_type": "bearer"}

# Dependency to get current user
from fastapi.security import OAuth2PasswordBearer

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")

async def get_current_user(token: str = Depends(oauth2_scheme)):
    credentials_exception = HTTPException(status_code=401, detail="Could not validate credentials")
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
    user = await get_user_by_email(email)
    if user is None:
        raise credentials_exception
    return {"id": str(user["_id"]), "email": user["email"], "name": user.get("name")}

@app.get("/me", response_model=UserOut)
async def me(current = Depends(get_current_user)):
    return {"id": current["id"], "email": current["email"], "name": current.get("name")}
