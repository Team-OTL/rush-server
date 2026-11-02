package main

import (
	"log"
	"net/http"

	"github.com/rush/rush-live/internal/config"
	"github.com/rush/rush-live/internal/router"
	"github.com/rush/rush-live/internal/ws"
)

func main() {
	cfg := config.Load()

	hub := ws.NewHub()
	go hub.Run()

	r := router.New(hub)

	log.Printf("rush-live listening on :%s", cfg.Port)
	log.Fatal(http.ListenAndServe(":"+cfg.Port, r))
}
