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

# Odata
od-meta:
	curl -v http://localhost:8080/odata/$$metadata | jq .

od-all:
	curl -v http://localhost:8080/odata/Todos | jq .

od-first:
	curl -v http://localhost:8080/odata/Todos\(1\) | jq .

od-first-title:
	curl -v http://localhost:8080/odata/Todos\(ID=1\)/Title | jq .

od-select:
	curl -v http://localhost:8080/odata/Todos\(1\)\?\$select=Title | jq .

od-select-all:
	curl -v http://localhost:8080/odata/Todos\(1\)\?\$select=\* | jq .

od-expand:
	curl -v http://localhost:8080/odata/Todos\(1\)\?\$expand=Tasks | jq .

od-expand-all:
	curl -v http://localhost:8080/odata/Todos\(1\)\?\$expand=\* | jq .

od-create:
	curl -v -X POST --json '{"Title":"test", "Description":"test"}' http://localhost:8080/odata/Todos | jq .
