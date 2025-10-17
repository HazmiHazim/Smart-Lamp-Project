from flask import Blueprint, request, jsonify
from api.db.database import get_db_connection
from datetime import datetime
import base64

device_type_bp = Blueprint("device_type_bp", __name__)

# Decorator to check API key from database
def require_api_key(f):
    def decorated(*args, **kwargs):
        encoded_key = request.headers.get("X-API-KEY")
        if not encoded_key:
            return jsonify({"error": "Missing API key"}), 401

        # Check database directly with encoded key
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

# ---------------- CRUD ROUTES ----------------

@device_type_bp.route("/", methods=["GET"])
@require_api_key
def get_device_types():
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM device_types")
    types = cursor.fetchall()
    cursor.close()
    conn.close()
    return jsonify(types)




@device_type_bp.route("/<int:type_id>", methods=["GET"])
@require_api_key
def get_device_type(type_id):
    conn = get_db_connection()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM device_types WHERE id=%s", (type_id,))
    dtype = cursor.fetchone()
    cursor.close()
    conn.close()
    if dtype:
        return jsonify(dtype)
    return jsonify({"error": "Device type not found"}), 404



@device_type_bp.route("/", methods=["POST"])
@require_api_key
def add_device_type():
    data = request.json
    if not data or "created_by" not in data:
        return jsonify({"error": "Missing required field 'created_by'"}), 400

    type_name = data.get("type_name")
    if not type_name:
        return jsonify({"error": "Missing required field 'type_name'"}), 400

    created_at = datetime.now()
    created_by = data["created_by"]

    conn = get_db_connection()
    cursor = conn.cursor()

    # Check if device type already exists (case-insensitive)
    cursor.execute("SELECT 1 FROM device_types WHERE LOWER(type_name) = LOWER(%s)", (type_name,))
    if cursor.fetchone():
        cursor.close()
        conn.close()
        return jsonify({"error": "Device type already exists"}), 409

    # Insert new device type
    cursor.execute("""
        INSERT INTO device_types (type_name, created_at, created_by, modified_at, modified_by)
        VALUES (%s, %s, %s, %s, %s)
    """, (
        type_name,
        created_at,
        created_by,
        created_at,
        created_by
    ))

    conn.commit()
    type_id = cursor.lastrowid
    cursor.close()
    conn.close()

    return jsonify({"message": "Device type added successfully", "id": type_id}), 201



@device_type_bp.route("/<int:type_id>", methods=["PUT"])
@require_api_key
def update_device_type(type_id):
    data = request.json
    if not data or "modified_by" not in data:
        return jsonify({"error": "Missing required field 'modified_by'"}), 400

    type_name = data.get("type_name")
    if not type_name:
        return jsonify({"error": "Missing required field 'type_name'"}), 400

    modified_at = datetime.now()
    modified_by = data["modified_by"]

    conn = get_db_connection()
    cursor = conn.cursor()

    # Check if device type exists
    cursor.execute("SELECT 1 FROM device_types WHERE id = %s", (type_id,))
    if not cursor.fetchone():
        cursor.close()
        conn.close()
        return jsonify({"error": "Device type not found"}), 404

    # Update device type
    cursor.execute("""
        UPDATE device_types
        SET type_name = %s,
            modified_at = %s,
            modified_by = %s
        WHERE id = %s
    """, (
        type_name,
        modified_at,
        modified_by,
        type_id
    ))

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Device type updated successfully"}), 200




@device_type_bp.route("/<int:type_id>", methods=["DELETE"])
@require_api_key
def delete_device_type(type_id):
    conn = get_db_connection()
    cursor = conn.cursor()

    # Check if device type exists
    cursor.execute("SELECT 1 FROM device_types WHERE id = %s", (type_id,))
    if not cursor.fetchone():
        cursor.close()
        conn.close()
        return jsonify({"error": "Device type not found"}), 404

    # Proceed with deletion
    cursor.execute("DELETE FROM device_types WHERE id = %s", (type_id,))
    conn.commit()

    cursor.close()
    conn.close()
    return jsonify({"message": "Device type deleted successfully"}), 200

