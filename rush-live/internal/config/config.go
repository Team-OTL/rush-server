package config

import "os"

type Config struct {
	Port string
}

func Load() Config {
	return Config{
		Port: getenv("PORT", "8087"),
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
