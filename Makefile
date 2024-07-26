define JSON_TODO
curl -X 'POST' \
  'http://localhost:8080/todo' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "description": "Todo string",
  "title": "Todo string"
}'
endef
export JSON_TODO

define JSON_TASK
curl -X 'POST' \
  'http://localhost:8080/todo/1/task' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "description": "Task string",
  "done": true,
  "title": "Task string"
}'
endef
export JSON_TASK

# Tools
todo:
	@echo $$JSON_TODO | bash

task:
	@echo $$JSON_TASK | bash

init: todo task

todo-list:
	@curl -X 'GET' 'http://localhost:8080/todo' -H 'accept: */*' | jq .

task-list:
	@curl -X 'GET' 'http://localhost:8080/todo/1/task' -H 'accept: */*' | jq .
