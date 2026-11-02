package main

import (
	"log"
	"net/http"

	"github.com/rush/rush-chat/internal/config"
	"github.com/rush/rush-chat/internal/router"
	"github.com/rush/rush-chat/internal/ws"
)

func main() {
	cfg := config.Load()

	hub := ws.NewHub()
	go hub.Run()

	r := router.New(hub)

	log.Printf("rush-chat listening on :%s", cfg.Port)
	log.Fatal(http.ListenAndServe(":"+cfg.Port, r))
}
