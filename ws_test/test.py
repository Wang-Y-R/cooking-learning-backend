import websocket

ws = websocket.WebSocket()
ws.connect("ws://127.0.0.1:8080/ws")
ws.send("hello")
print(ws.recv())
ws.close()




