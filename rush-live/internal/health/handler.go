package health

import (
	"encoding/json"
	"net/http"
)

func Ping(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]string{"service": "rush-live", "status": "ok"})
}
