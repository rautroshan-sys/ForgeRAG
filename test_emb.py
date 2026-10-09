import requests
import os
import json

api_key = open('/home/roshan/ForgeRAG/.env').read().split('GEMINI_API_KEY=')[1].split('\n')[0]
url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key={api_key}"
data = {
  "model": "models/gemini-embedding-001",
  "content": {
    "parts": [{
      "text": "Hello world"
    }]
  }
}
res = requests.post(url, json=data)
print(res.json())
