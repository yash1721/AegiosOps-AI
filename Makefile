.PHONY: backend frontend test docker-up docker-down seed demo

backend:
	cd backend && mvn spring-boot:run

frontend:
	cd frontend && npm install && npm run dev

test:
	cd backend && mvn test
	cd frontend && npm run build

docker-up:
	docker compose up --build

docker-down:
	docker compose down

seed:
	curl -X POST http://localhost:8080/api/v1/dev/seed

demo:
	bash scripts/demo-flow.sh
