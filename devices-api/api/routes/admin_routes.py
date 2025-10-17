from flask import Blueprint, request, jsonify
import os
import base64
import secrets
from datetime import datetime
from api.db.database import get_db_connection  # your DB connection function

SECRET_KEY = os.getenv("SECRET_KEY")  # already loaded from config

admin_bp = Blueprint("admin_bp", __name__)

def require_env_secret_key(f):
    """Decorator to check the .env SECRET_KEY for access."""
    def decorated(*args, **kwargs):
        header_key = request.headers.get("X-SECRET-KEY")
        if not header_key:
            return jsonify({"error": "Missing secret key"}), 401
        if header_key != SECRET_KEY:
            return jsonify({"error": "Unauthorized"}), 401
        return f(*args, **kwargs)
    
    decorated.__name__ = f.__name__
    return decorated

@admin_bp.route("/generate_api_key", methods=["POST"])
@require_env_secret_key
def generate_api_key():
    data = request.json
    if not data or "userid" not in data:
        return jsonify({"error": "Missing required parameter 'userid'"}), 400

    created_by = data["userid"]

    # Generate a random token
    raw_key = secrets.token_urlsafe(32)
    encoded_key = base64.b64encode(raw_key.encode("utf-8")).decode("utf-8")

    # Save to database
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO api_keys (api_key_name, created_at, created_by)
        VALUES (%s, %s, %s)
    """, (
        encoded_key,
        datetime.now(),  # store as DATETIME
        created_by
    ))
    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"api_key": encoded_key})
