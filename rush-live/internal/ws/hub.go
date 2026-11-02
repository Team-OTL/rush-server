package ws

import (
	"log"
	"net/http"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	// 게이트웨이 뒤에 있으므로 Origin 검사는 게이트웨이/CORS 설정에 맡깁니다.
	CheckOrigin: func(*http.Request) bool { return true },
}

type client struct {
	userID string
	conn   *websocket.Conn
	send   chan []byte
}

// Hub는 접속한 모든 클라이언트에게 메시지를 브로드캐스트하는 가장 단순한 구조입니다.
type Hub struct {
	clients    map[*client]struct{}
	register   chan *client
	unregister chan *client
	broadcast  chan []byte
}

func NewHub() *Hub {
	return &Hub{
		clients:    make(map[*client]struct{}),
		register:   make(chan *client),
		unregister: make(chan *client),
		broadcast:  make(chan []byte),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case c := <-h.register:
			h.clients[c] = struct{}{}
		case c := <-h.unregister:
			if _, ok := h.clients[c]; ok {
				delete(h.clients, c)
				close(c.send)
			}
		case msg := <-h.broadcast:
			for c := range h.clients {
				select {
				case c.send <- msg:
				default:
					delete(h.clients, c)
					close(c.send)
				}
			}
		}
	}
}

func (h *Hub) ServeWS(w http.ResponseWriter, r *http.Request) {
	// 게이트웨이에서 JWT 검증 후 넣어준 헤더
	userID := r.Header.Get("X-User-Id")
	if userID == "" {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("upgrade: %v", err)
		return
	}

	c := &client{userID: userID, conn: conn, send: make(chan []byte, 256)}
	h.register <- c

	go c.writeLoop()
	c.readLoop(h)
}

func (c *client) readLoop(h *Hub) {
	defer func() {
		h.unregister <- c
		_ = c.conn.Close()
	}()
	for {
		_, msg, err := c.conn.ReadMessage()
		if err != nil {
			return
		}
		h.broadcast <- msg
	}
}

func (c *client) writeLoop() {
	for msg := range c.send {
		if err := c.conn.WriteMessage(websocket.TextMessage, msg); err != nil {
			return
		}
	}
}
