package main

import (
	"context"
	"flag"
	log "github.com/sirupsen/logrus"
	"net"
	"strings"
	"bytes"
	"sync"
	"time"
)

var lock sync.Mutex

var ok = []byte(`HTTP/1.1 200 OK
Server: HA-Counter (github.com/kiselev-nikolay/ha-counter)
Connection: keep-alive
Cache-Control: no-store
Access-Control-Allow-Origin: *
Content-Type: text/plain
Content-Length: 2

ok`)

var fail = []byte(`HTTP/1.1 503 Service Unavailable
Server: HA-Counter (github.com/kiselev-nikolay/ha-counter)
Connection: keep-alive
Cache-Control: no-store
Access-Control-Allow-Origin: *
Content-Type: text/plain
Content-Length: 4

fail`)

type Event struct {
	Type string
	ID   string
}

var count map[Event]bool
var timer map[Event]int64

func coldStorage() {
	for {
		t := time.Now().Unix()
		lock.Lock()
		countCopy := count
		count = make(map[Event]bool)
		lock.Unlock()
		for k, _ := range countCopy {
			timer[k] = t
		}
		// log.Debug(timer)
		time.Sleep(10 * time.Second)
	}
}

func processConnection(connection net.Conn, ctx context.Context) {
	defer func() {
		defer func() {
			recover()
			return
		}()
		recover()
		connection.Write(fail)
		connection.Close()
		return
	}()
	select {
	default:
		request := make([]byte, 255)
		_, err := connection.Read(request)
		if err != nil {
			connection.Write(fail)
			connection.Close()
			return
		}
		lineEnd := bytes.IndexAny(request, "\n")
		netData := string(request[:lineEnd])
		path := strings.Fields(netData)[1]
		path = strings.Trim(path, "/")
		pathParts := strings.Split(path, "/")
		lock.Lock()
		defer lock.Unlock()
		e := Event{
			Type: pathParts[0],
			ID:   pathParts[1],
		}
		// log.Debug(e)
		count[e] = true
		connection.Write(ok)
		connection.Close()
		return
	case <-ctx.Done():
		connection.Write(fail)
		connection.Close()
		return
	}
}

func main() {
	host := flag.String("host", "127.0.0.1", "Hostname binding")
	port := flag.String("port", "8078", "TCP port binding")
	isJson := flag.Bool("json", false, "Use only json logging")
	isVerbose := flag.Bool("v", false, "Use verbose logging")
	flag.Parse()

	if *isJson {
		log.SetFormatter(&log.JSONFormatter{})
	}

	if *isVerbose {
		log.SetLevel(log.DebugLevel)
	}

	log.WithFields(log.Fields{
		"host": *host,
		"port": *port,
	}).Info("Starting server")

	count = make(map[Event]bool)
	timer = make(map[Event]int64)

	l, err := net.Listen("tcp", *host+":"+*port)
	if err != nil {
		log.Error(err)
		return
	}
	defer l.Close()

	go coldStorage()

	for {
		ctx, cancel := context.WithCancel(context.Background())
		connection, _ := l.Accept()
		// ip := connection.RemoteAddr()
		go processConnection(connection, ctx)
		go func() {
			<-time.After(2 * time.Second)
			cancel()
		}()
	}
}
