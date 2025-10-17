import mysql.connector
from mysql.connector import errorcode
import secrets
from datetime import datetime, timezone
from config import DB_HOST, DB_USER, DB_PASSWORD, DB_NAME, DB_PORT

# Function to generate short ID (~8 characters)
def generate_short_id():
    return secrets.token_urlsafe(6)  # gives ~8 chars

try:
    # Connect to MySQL server (without database)
    conn = mysql.connector.connect(
        host=DB_HOST,
        user=DB_USER,
        password=DB_PASSWORD,
        port=DB_PORT
    )
    cursor = conn.cursor()

    # 1. Create database if not exists
    cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    print(f"Database '{DB_NAME}' ensured.")

    cursor.close()
    conn.close()

    # Connect to the specific database
    conn = mysql.connector.connect(
        host=DB_HOST,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        port=DB_PORT
    )
    cursor = conn.cursor()

    # 2. Create device_types table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS device_types (
        id INT AUTO_INCREMENT PRIMARY KEY,
        type_name VARCHAR(255) NOT NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(255) NOT NULL,
        modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        modified_by VARCHAR(255)
    ) ENGINE=InnoDB;
    """)
    print("Table 'device_types' ensured.")

    # 3. Create devices table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS devices (
        id INT AUTO_INCREMENT PRIMARY KEY,
        public_id VARCHAR(255) NOT NULL,
        name VARCHAR(255) NOT NULL,
        model VARCHAR(255),
        serial_no VARCHAR(255),
        device_type INT,
        tx_key VARCHAR(255) NOT NULL,
        rx_key VARCHAR(255) NOT NULL,
        image_path VARCHAR(255),
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(255) NOT NULL,
        modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        modified_by VARCHAR(255),
        FOREIGN KEY (device_type) REFERENCES device_types(id)
            ON DELETE SET NULL
            ON UPDATE CASCADE
    ) ENGINE=InnoDB;
    """)
    print("Table 'devices' ensured.")

    # 4. Create api_keys table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS api_keys (
        id INT AUTO_INCREMENT PRIMARY KEY,
        api_key_name VARCHAR(255) NOT NULL,
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(255) NOT NULL
    ) ENGINE=InnoDB;
    """)
    print("Table 'api_keys' ensured.")

    conn.commit()
    cursor.close()
    conn.close()

    print("Database initialization completed successfully!")

except mysql.connector.Error as err:
    if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
        print("Something is wrong with your user name or password")
    elif err.errno == errorcode.ER_BAD_DB_ERROR:
        print("Database does not exist")
    else:
        print(err)
