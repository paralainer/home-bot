import json
from http.server import BaseHTTPRequestHandler
from http.server import HTTPServer
import subprocess


def speedtest():
    print("Measuring speed\n")
    out = subprocess.Popen(['speedtest', '--simple'], stdout=subprocess.PIPE)
    stdout, stderr = out.communicate()
    string = stdout.decode("utf-8")
    print(string)
    parts = string.splitlines()
    ping = parts[0].strip().split()[1]
    download = parts[1].strip().split()[1]
    upload = parts[2].strip().split()[1]
    return json.dumps({"ping": ping, "download": download, "upload": upload})


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        speed = speedtest()

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(speed.encode("utf-8"))


print("Starting server...\n")
httpd = HTTPServer(('', 8000), Handler)
httpd.serve_forever()
