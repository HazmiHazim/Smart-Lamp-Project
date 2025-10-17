from flask import Blueprint, request, jsonify
from api.db.database import get_db_connection
from datetime import datetime
import secrets

device_bp = Blueprint("device_bp", __name__)

# Decorator to check API key from database
def require_api_key(f):
    def decorated(*args, **kwargs):
        encoded_key = request.headers.get("X-API-KEY")
        if not encoded_key:
            return jsonify({"error": "Missing API key"}), 401

        encoded_key = encoded_key.strip()

        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM api_keys WHERE api_key_name = %s", (encoded_key,))
        key_exists = cursor.fetchone()
        cursor.close()
        conn.close()

        if not key_exists:
            return jsonify({"error": "Unauthorized"}), 401

        return f(*args, **kwargs)

    decorated.__name__ = f.__name__
    return decorated

# Helper to generate short device ID (~8 chars)
def generate_device_id():
    return secrets.token_urlsafe(6)

# ---------------- CRUD ROUTES ----------------

@device_bp.route("/", methods=["GET"])
@require_api_key
def get_devices():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM devices")
    devices = cursor.fetchall()
    cursor.close()
    conn.close()
    return jsonify(devices)

@device_bp.route("/<public_id>", methods=["GET"])
@require_api_key
def get_device(public_id):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM devices WHERE public_id=%s", (public_id,))
    device = cursor.fetchone()
    cursor.close()
    conn.close()
    if device:
        return jsonify(device)
    return jsonify({"error": "Device not found"}), 404

@device_bp.route("/", methods=["POST"])
@require_api_key
def add_device():
    data = request.json

    # Require created_by
    created_by = data.get("created_by")
    if not created_by:
        return jsonify({"error": "Missing required field 'created_by'"}), 400

    public_id = data.get("public_id")
    if not public_id:
        return jsonify({"error": "Missing required field 'public_id'"}), 400

    now = datetime.now()

    conn = get_db_connection()
    cursor = conn.cursor()

    # Check if public_id already exists
    cursor.execute("SELECT 1 FROM devices WHERE public_id = %s", (public_id,))
    if cursor.fetchone():
        cursor.close()
        conn.close()
        return jsonify({"error": "Device already exists"}), 409

    # Insert new device
    cursor.execute("""
        INSERT INTO devices 
        (public_id, name, model, serial_no, device_type, tx_key, rx_key, image_path, created_at, created_by, modified_at, modified_by)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, (
        public_id,
        data.get("name"),
        data.get("model"),
        data.get("serial_no"),
        data.get("device_type"),
        data.get("tx_key"),
        data.get("rx_key"),
        data.get("image_path"),
        now,
        created_by,
        now,
        created_by
    ))

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Device added successfully"}), 201



@device_bp.route("/<device_id>", methods=["PUT"])
@require_api_key
def update_device(device_id):
    data = request.json

    # Require modified_by
    modified_by = data.get("modified_by")
    if not modified_by:
        return jsonify({"error": "Missing required field 'modified_by'"}), 400

    now = datetime.now()

    conn = get_db_connection()
    cursor = conn.cursor()

    # Check if device exists
    cursor.execute("SELECT 1 FROM devices WHERE id = %s", (device_id,))
    if not cursor.fetchone():
        cursor.close()
        conn.close()
        return jsonify({"error": "Device not found"}), 404

    # Proceed with update
    cursor.execute("""
        UPDATE devices
        SET name = %s,
            model = %s,
            serial_no = %s,
            device_type = %s,
            tx_key = %s,
            rx_key = %s,
            image_path = %s,
            modified_at = %s,
            modified_by = %s
        WHERE id = %s
    """, (
        data.get("name"),
        data.get("model"),
        data.get("serial_no"),
        data.get("device_type"),
        data.get("tx_key"),
        data.get("rx_key"),
        data.get("image_path"),
        now,
        modified_by,
        device_id
    ))

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Device updated successfully"}), 200



@device_bp.route("/<device_id>", methods=["DELETE"])
@require_api_key
def delete_device(device_id):
    conn = get_db_connection()
    cursor = conn.cursor()

    # Check if device exists
    cursor.execute("SELECT 1 FROM devices WHERE id = %s", (device_id,))
    if not cursor.fetchone():
        cursor.close()
        conn.close()
        return jsonify({"error": "Device not found"}), 404

    # Proceed to delete
    cursor.execute("DELETE FROM devices WHERE id = %s", (device_id,))
    conn.commit()

    cursor.close()
    conn.close()
    return jsonify({"message": "Device deleted successfully"}), 200

