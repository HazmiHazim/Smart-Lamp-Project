from flask import Flask
from api.routes.device_routes import device_bp
from api.routes.device_type_routes import device_type_bp
from api.routes.admin_routes import admin_bp

# Create Flask app
app = Flask(__name__)
app.config['DEBUG'] = True  # optional, enable debug mode

# Register blueprint with a URL prefix
app.register_blueprint(device_bp, url_prefix="/api/devices")
app.register_blueprint(device_type_bp, url_prefix="/api/device_types")
app.register_blueprint(admin_bp, url_prefix="/api/admin")

# Run app
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
