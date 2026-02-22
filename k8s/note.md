kubectl apply -f k8s/<file_name>.yaml
kubectl delete -f k8s/<file_name>.yaml


## GEt logs
kubctl logs <pod-name> 

## Exec into pod
kubectl exec -it <pod-name> -- /bin/bash

## Get services
kubectl get services
kubectl get svc

kubectl get pods



## Port Forwarding
kubectl port-forward <pod-name> 3306:3306


