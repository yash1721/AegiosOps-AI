.PHONY: backend frontend test docker-up docker-down seed demo

backend:
	cd backend && ./mvnw spring-boot:run

frontend:
	cd frontend && npm run dev

test:
	cd backend && ./mvnw test
	cd frontend && npm run build

docker-up:
	docker compose up -d postgres redis qdrant

docker-down:
	docker compose down

seed:
	@echo "Seed data command is not implemented yet."

demo:
	@echo "Demo command is not implemented yet."
