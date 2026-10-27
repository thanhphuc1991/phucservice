#!/bin/bash

echo "=== Build code ==="
mvn clean install -DskipTests

echo "=== Build images ==="
mvn jib:dockerBuild -pl OrderService,InventoryService,PaymentService,ShipmentService

echo "=== Load vào Minikube ==="
minikube image load phucservice/order-service:latest
minikube image load phucservice/inventory-service:latest
minikube image load phucservice/payment-service:latest
minikube image load phucservice/shipment-service:latest
minikube image load apache/kafka:4.0.0

echo "=== Deploy ==="
helm upgrade --install phucservice helm/

echo "=== Status ==="
kubectl get pods