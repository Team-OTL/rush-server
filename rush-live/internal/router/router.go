package router

import (
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/rush/rush-live/internal/health"
	"github.com/rush/rush-live/internal/ws"
)

func New(hub *ws.Hub) http.Handler {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	// 게이트웨이가 /live/** 를 그대로 넘겨줍니다.
	r.Route("/live", func(r chi.Router) {
		r.With(middleware.Timeout(10*time.Second)).Get("/ping", health.Ping)

		// WebSocket은 연결이 오래 유지되므로 Timeout 미들웨어를 걸지 않습니다.
		// 예) ws://localhost:8080/live/ws?token=<JWT>
		r.Get("/ws", hub.ServeWS)
	})

	return r
}
