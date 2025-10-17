from dotenv import load_dotenv
import os

load_dotenv() 

DB_HOST = os.getenv("DB_HOST", "localhost")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "lampdb")
DB_PORT = int(os.getenv("DB_PORT", 3306))