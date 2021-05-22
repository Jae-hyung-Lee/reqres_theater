# 2조 과제 : 영화예매 시스템
![image](https://user-images.githubusercontent.com/80744278/119227825-d0ccf880-bb4a-11eb-9f53-99faf04a0f35.png)

# 서비스 시나리오

기능적 요구사항
1. 고객이 영화를 선택하여 예매한다.
1. 고객이 결제한다.
1. 예매가 완료되면 예매 내역이 해당 극장에 전달된다.
1. 영화가 등록되면 상영관이 지정된다.
1. 해당 극장은 해당영화의 관람관 좌석를 예매처리 한다.
1. 고객은 예매를 취소할 수 있다.
1. 예매를 취소하면 좌석예매도 취소처리한다.
1. 고객은 예매현황을 조회할 수있다.
1. 예매 현황은 카톡으로 알려 준다.(예매 취소 포함)

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않으면 예매를 할 수 없다.(Sync호출)
1. 장애격리
    1. 예매관리 기능이 수행되지 않더라도 예매는 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다  Circuit breaker, fallback
1. 성능
    1. 고객이 자주 예매관리에서 확인할 수 있는 예매현황을 예매시스템(프론트엔드)에서 확인할 수 있어야 한다  CQRS
    1. 예매상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven


# 체크포인트

1. Saga
1. CQRS
1. Correlation
1. Req/Resp
1. Gateway
1. Deploy/ Pipeline
1. Circuit Breaker
1. Autoscale (HPA)
1. Zero-downtime deploy (Readiness Probe)
1. Config Map/ Persistence Volume
1. Polyglot
1. Self-healing (Liveness Probe)

# 분석/설계
![image](https://user-images.githubusercontent.com/80908892/118935502-94509f80-b986-11eb-820d-7ad60bf637a0.png)

- movie서비스에서 cutomer조회 기능 제거
- app서비스에서 pay서비스로가는 cancel(pub/sub) 중복 표현 제거(3개 -> 1개)
![image](https://user-images.githubusercontent.com/80908892/118985614-8e27e680-b9b9-11eb-9b81-fe8d1196887e.png)

## 헥사고날 아키텍처 다이어그램 도출 (Polyglot)
![핵사고날_최종](https://user-images.githubusercontent.com/78134019/109744745-29f55200-7c16-11eb-8981-88924ad28cb3.jpg)

# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd app
mvn spring-boot:run

cd pay
mvn spring-boot:run 

cd movie
mvn spring-boot:run  

cd theater
mvn spring-boot:run

cd notice
mvn spring-boot:run
```

## DDD 의 적용
총 5개의 Domain 으로 관리되고 있으며, 예약관리(Reservation) , 결재관리(Approval), 상영영화(MovieManagement), 영화좌석관리(MovieSeat), 영화관리(Movie)으로 구성하였습니다. 

```
package theater;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

@Entity
@Table(name="MovieManagement_table")
public class MovieManagement {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long movieId;
    private String title;
    private String status;

    @PostPersist
    public void onPostPersist(){
        MovieRegistered movieRegistered = new MovieRegistered();
        BeanUtils.copyProperties(this, movieRegistered);
        movieRegistered.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package theater;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="movieManagements", path="movieManagements")
public interface MovieManagementRepository extends PagingAndSortingRepository<MovieManagement, Long>{

}
```

- 적용 후 REST API 의 테스트
```
# Movie 등록
http POST http://localhost:8083/movieManagements movieId=10001 title="분노의 질주" status="opened"
http POST http://localhost:8083/movieManagements movieId=10002 title="미션 파서블" status="opened"
http POST http://localhost:8083/movieManagements movieId=10003 title="자산어보" status="opened"
http POST http://localhost:8083/movieManagements movieId=10004 title="간이역" status="opened"
http GET http://localhost:8083/movieManagements
http GET http://localhost:8084/movies
```

## 폴리글랏 퍼시스턴스

```
위치 : /reqres_theater>app>pom.xml
```
![image](https://user-images.githubusercontent.com/80744278/119217655-83369880-bb16-11eb-95f9-588fcfcebcbe.png)

## ★ 수정 필요 ★ 폴리글랏 프로그래밍 

영화 예매 서비스의 시나리오인 예약 완료/취소 상태 변경에 따라 고객에게 카톡메시지 보내는 기능의 구현 하였다. 구현체는 각 이벤트를 수신하여 처리하는 Kafka consumer 로 구현되었고 코드는 다음과 같다:
```
from flask import Flask
from redis import Redis, RedisError
from kafka import KafkaConsumer
import os
import socket


# To consume latest messages and auto-commit offsets
consumer = KafkaConsumer('fooddelivery',
                         group_id='',
                         bootstrap_servers=['localhost:9092'])
for message in consumer:
    print ("%s:%d:%d: key=%s value=%s" % (message.topic, message.partition,
                                          message.offset, message.key,
                                          message.value))

    # 카톡호출 API
```

애플리케이션을 컴파일하고 실행하기 위한 도커파일은 아래와 같다
```
FROM python:2.7-slim
WORKDIR /app
ADD . /app
RUN pip install --trusted-host pypi.python.org -r requirements.txt
ENV NAME World
EXPOSE 8090
CMD ["python", "policy-handler.py"]
```

## 마이크로 서비스 호출 흐름

- 영화 예매 처리 

고객이 영화를 선택 후 예매(app) 후 결재요청(pay)은 (req/res) 되며,
결재 완료 후 해당 극장(theater) 좌석 선택 후 예매가 완료 됩니다.

고객이 영화를 선택 합니다. 
```
http POST http://localhost:8083/movieManagements movieId=10001 title="분노의 질주" status="opened"
http POST http://localhost:8083/movieManagements movieId=10002 title="미션 파서블" status="opened"
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


영화 선택 후 결재처리 합니다.   
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

결재가 완료 되면 해당 극장에서 영화와 관람관 좌석을 선택하여 예매를 완료 합니다. 
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

- 영화 예매 취소 처리

고객이 예약 현황(app)을 확인 하고 해당 예약을 취소하면 결재(pay) 및 
해당 극장(theater) 예약이 취소 됩니다. 

고객이 예약 취소를 요청 합니다.
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

예약 취소 요청를 완료하면 결재를 취소를 합니다. 
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

결재 취소가 되면 해당 극장 영화/좌석의 예약이 취소 됩니다.  
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


- 고객 메시지 서비스 처리 

고객은 예약 완료, 예약 취소에 대한 메시지를 다음과 같이 받을 수 있으며, 관련 정보를 또한 확인 할 수 있습니다.

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


## Gateway 적용

서비스에 대한 하나의 접점을 만들기 위한 게이트웨이의 설정은 8088이며, 
예매관리,결재관리 및 극장정보등 마이크로서비스에 대한 일원화 된 접점을 제공하기 위한 설정 입니다.
```
app 서비스 : 8081
pay 서비스 : 8082
movie 서비스 : 8083
theater 서비스 : 8084
notice 서비스 : 8086
```

gateway > applitcation.yml 설정

![image](https://user-images.githubusercontent.com/80744278/119226171-c0188480-bb42-11eb-9039-dd83a40e9b9d.png)

- gateway 로컬 테스트

로컬 테스트는 다음과 같이 한글 서비스 호출로 테스트 되었습니다.

```
http localhost:8080/app
-> gateway 를 호출하나 8081 로 호출됨
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


## 동기식 호출 과 Fallback 처리

영화 예약(app)후 결재처리(pay) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하였습니다.
REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 
```
 - 동기식 호출 영화 예약 후 결재 처리 관련 소스 추가
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

```
 - Fallback 처리 결과 
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)



## 비동기식 호출 / 장애격리 / 성능

영화 예매 취소(app) 요청 이후 결재 취소(pay), 해당 극장 영화/좌석 취소(theater)는 비동기식 처리이므로, 
영화 예매 취소(app)의 서비스 호출에는 영향이 없도록 구성 합니다.

영화 예매 취소(app) 요청 후 결재 취소(pay)가 정상적으로 안되는 경우 
해당 극장 영화/좌석(theater)은 예약중으로 남게 됩니다.

<영화 예매 취소 요청>

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

<극장 영화/좌석 예약 상태>

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

## 정보 조회 / View 조회
고객은 예약 현황을 조회 할 수 있습니다. 

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)



## 소스 패키징

- 클라우드 배포를 위해서 다음과 같이 패키징 작업을 하였습니다.
```
cd app
mvn clean && mvn package
cd ..
cd pay
mvn clean && mvn package
cd ..
cd movie
mvn clean && mvn package
cd ..
cd theater
mvn clean && mvn package
cd ..
cd notice
mvn clean && mvn package
cd ..
```
	
![image](https://user-images.githubusercontent.com/80744278/119227564-8a2ace80-bb49-11eb-88ce-e2df20a617a4.png)


# ★ 수정 필요 ★ 클라우드 배포/운영 파이프라인

- aws 클라우드에 배포하기 위해서 다음과 같이 주요 정보를 설정 하였습니다.

```
리소스 그룹명 : skccteam03-rsrcgrp
클러스터 명 : skccteam03-aks
레지스트리 명 : skccteam03
```

- aws login
우선 aws에 로그인 합니다.
```
{
    "cloudName": "AzureCloud",
    "homeTenantId": "6011e3f8-2818-42ea-9a63-66e6acc13e33",
    "id": "718b6bd0-fb75-4ec9-9f6e-08ae501f92ca",
    "isDefault": true,
    "managedByTenants": [],
    "name": "2",
    "state": "Enabled",
    "tenantId": "6011e3f8-2818-42ea-9a63-66e6acc13e33",
    "user": {
      "name": "skTeam03@gkn2021hotmail.onmicrosoft.com",
      "type": "user"
    }
  }
```

- 토큰 가져오기
```
az aks get-credentials --resource-group skccteam03-rsrcgrp --name skccteam03-aks
```

- aks에 acr 붙이기
```
az aks update -n skccteam03-aks -g skccteam03-rsrcgrp --attach-acr skccteam03
```

![aks붙이기](https://user-images.githubusercontent.com/78134019/109653395-540e2c00-7ba4-11eb-97dd-2dcfdf5dc539.jpg)

- 네임스페이스 만들기

```
kubectl create ns team03
kubectl get ns
```
![image](https://user-images.githubusercontent.com/78134019/109776836-5cb73e80-7c46-11eb-9562-d462525d6dab.png)

* 도커 이미지 만들고 레지스트리에 등록하기
```
cd taxicall_eng
az acr build --registry skccteam03 --image skccteam03.azurecr.io/taxicalleng:v1 .
az acr build --registry skccteam03 --image skccteam03.azurecr.io/taxicalleng:v2 .
cd ..
cd taximanage_eng
az acr build --registry skccteam03 --image skccteam03.azurecr.io/taximanageeng:v1 .
cd ..
cd taxiassign_eng
az acr build --registry skccteam03 --image skccteam03.azurecr.io/taxiassigneng:v1 .
cd ..
cd gateway_eng
az acr build --registry skccteam03 --image skccteam03.azurecr.io/gatewayeng:v1 .
cd ..
cd customer_py
az acr build --registry skccteam03 --image skccteam03.azurecr.io/customer-policy-handler:v1 .
```

![docker_gateway](https://user-images.githubusercontent.com/78134019/109777813-76a55100-7c47-11eb-8d8d-59eaabefab54.png)

![docker_taxiassign](https://user-images.githubusercontent.com/78134019/109777820-77d67e00-7c47-11eb-9d77-85403dcf2da4.png)

![docker_taxicall](https://user-images.githubusercontent.com/78134019/109777826-786f1480-7c47-11eb-9992-41f75907d16f.png)

![docker_taximanage](https://user-images.githubusercontent.com/78134019/109777827-786f1480-7c47-11eb-9c9b-d3357eda0bd5.png)

![docker_customer](https://user-images.githubusercontent.com/78134019/109777829-7907ab00-7c47-11eb-936f-723396cb272a.png)


-각 마이크로 서비스를 yml 파일을 사용하여 배포 합니다.


![deployment_yml](https://user-images.githubusercontent.com/78134019/109652001-9171ba00-7ba2-11eb-8c29-7128ceb4ec97.jpg)

- deployment.yml로 서비스 배포
```
cd ../../
cd customer_py/kubernetes
kubectl apply -f deployment.yml --namespace=team03
kubectl apply -f service.yaml --namespace=team03
cd ../../
cd taxicall_eng/kubernetes
kubectl apply -f deployment.yml --namespace=team03
kubectl apply -f service.yaml --namespace=team03

cd ../../
cd taximanage_eng/kubernetes
kubectl apply -f deployment.yml --namespace=team03
kubectl apply -f service.yaml --namespace=team03

cd ../../
cd taxiassign_eng/kubernetes
kubectl apply -f deployment.yml --namespace=team03
kubectl apply -f service.yaml --namespace=team03

cd ../../
cd gateway_eng/kubernetes
kubectl apply -f deployment.yml --namespace=team03
kubectl apply -f service.yaml --namespace=team03
```
<Deploy cutomer>
	
![deploy_customer](https://user-images.githubusercontent.com/78134019/109744443-a471a200-7c15-11eb-94c9-a0c0a7999d04.png)

<Deploy gateway>
	
![deploy_gateway](https://user-images.githubusercontent.com/78134019/109744457-acc9dd00-7c15-11eb-8502-ff65e779e9d2.png)

<Deploy taxiassign>
	
![deploy_taxiassign](https://user-images.githubusercontent.com/78134019/109744471-b3585480-7c15-11eb-8d68-bba9c3d8ce01.png)

<Deploy taxicall>
	
![deploy_taxicall](https://user-images.githubusercontent.com/78134019/109744487-bb17f900-7c15-11eb-8bd0-ff0a9fc9b2e3.png)


![deploy_taximanage](https://user-images.githubusercontent.com/78134019/109744591-e69ae380-7c15-11eb-834a-44befae55092.png)



- 서비스확인
```
kubectl get all -n team03
```
![image](https://user-images.githubusercontent.com/78134019/109777026-9be58f80-7c46-11eb-9eac-a55ebcf91989.png)



## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현하였습니다.

- Hystrix 를 설정:  

요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml
feign:
  hystrix:
    enabled: true

# To set thread isolation to SEMAPHORE
#hystrix:
#  command:
#    default:
#      execution:
#        isolation:
#          strategy: SEMAPHORE

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```
![hystrix](https://user-images.githubusercontent.com/78134019/109652345-0218d680-7ba3-11eb-847b-708ba071c119.jpg)


부하테스트


* Siege 리소스 생성

```
kubectl run siege --image=apexacme/siege-nginx -n team03
```

* 실행

```
kubectl exec -it pod/siege-5459b87f86-hlfm9 -c siege -n team03 -- /bin/bash
```

*부하 실행

```
siege -c200 -t60S -r10 -v --content-type "application/json" 'http://20.194.36.201:8080/taxicalls POST {"tel": "0101231234"}'
```

- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 택시호출(taxicall) 서비스에서 처리되면서 
다시 taxicall에서 서비스를 받기 시작 합니다

![secs1](https://user-images.githubusercontent.com/78134019/109786899-01d71480-7c51-11eb-9e6c-0a819e85b020.png)


- report

![secs2](https://user-images.githubusercontent.com/78134019/109786922-07345f00-7c51-11eb-900a-315f7d0d6484.png)





### 오토스케일 아웃



```
# autocale out 설정
 deployment.yml 설정
```


![auto1](https://user-images.githubusercontent.com/78134019/109794479-3ea70980-7c59-11eb-8d32-fbc039106c8c.jpg)


```
kubectl autoscale deploy taxicall --min=1 --max=10 --cpu-percent=15 -n team03
```


```
root@labs--279084598:/home/project# kubectl exec -it pod/siege-5459b87f86-hlfm9 -c siege -n team03 -- /bin/bash
root@siege-5459b87f86-hlfm9:/# siege -c100 -t120S -r10 -v --content-type "application/json" 'http://20.194.36.201:8080/taxicalls POST {"tel": "0101231234"}'
```
![auto4](https://user-images.githubusercontent.com/78134019/109794919-b70dca80-7c59-11eb-9710-8ff6b4dd5f54.jpg)



- 오토스케일링에 대한 모니터링:
```
kubectl get deploy taxicall -w -n team03
```
![auto_final](https://user-images.githubusercontent.com/78134019/109796515-98a8ce80-7c5b-11eb-9512-a0a927217a38.jpg)



## 무정지 재배포

- deployment.yml에 readiness 옵션을 추가 


![무정지 배포1](https://user-images.githubusercontent.com/78134019/109809110-45d71300-7c6b-11eb-955c-9b8a3b3db698.png)


- seige 실행
```
siege -c100 -t120S -r10 -v --content-type "application/json" 'http://20.194.36.201:8080/taxicalls POST {"tel": "0101231234"}'
```


- Availability: 100.00 % 확인


![무정지 배포2](https://user-images.githubusercontent.com/78134019/109810318-bd597200-7c6c-11eb-88e4-197386b1e338.png)


![무정지 배포3](https://user-images.githubusercontent.com/78134019/109810688-2fca5200-7c6d-11eb-9c67-d252d703064a.png)



## Config Map

- apllication.yml 설정

* default 프로파일

![configmap1](https://user-images.githubusercontent.com/31096538/109798636-5df46580-7c5e-11eb-982d-16482f98b13f.JPG)

* docker 프로파일

![configmap2](https://user-images.githubusercontent.com/31096538/109798699-6e0c4500-7c5e-11eb-9d0d-47b90d637ae9.JPG)

- Deployment.yml 설정

![configmap3](https://user-images.githubusercontent.com/31096538/109798713-72d0f900-7c5e-11eb-8458-8fb9d6225c49.JPG)

- config map 생성 후 조회
```
kubectl create configmap apiurl --from-literal=url=http://taxicall:8080 --from-literal=fluentd-server-ip=10.xxx.xxx.xxx -n team03
```
![configmap4](https://user-images.githubusercontent.com/31096538/109798727-76fd1680-7c5e-11eb-9818-327870ea2e4d.JPG)

- 설정한 url로 주문 호출
```
http 20.194.36.201:8080/taxicalls tel="01012345678" status="call" location="mapo" cost=25000
```

![configmap5](https://user-images.githubusercontent.com/31096538/109798744-7c5a6100-7c5e-11eb-8aaa-03fa8277cee6.JPG)

- configmap 삭제 후 app 서비스 재시작
```
kubectl delete configmap apiurl -n team03
kubectl get pod/taxicall-74f7dbc967-mtbmq -n team03 -o yaml | kubectl replace --force -f-
```
![configmap6](https://user-images.githubusercontent.com/31096538/109798766-811f1500-7c5e-11eb-8008-1b9073cb6722.JPG)

- configmap 삭제된 상태에서 주문 호출   
```
http 20.194.36.201:8080/taxicalls tel="01012345678" status="call" location="mapo" cost=25000
kubectl get all -n team03
```
![configmap7](https://user-images.githubusercontent.com/31096538/109798785-85e3c900-7c5e-11eb-8769-ab416b1e17b2.JPG)


![configmap8](https://user-images.githubusercontent.com/31096538/109798805-8bd9aa00-7c5e-11eb-8d05-1db2457d3611.JPG)


![configmap9](https://user-images.githubusercontent.com/31096538/109798824-9005c780-7c5e-11eb-9d5b-6f14f9b6bba9.JPG)


## Self-healing (Liveness Probe)


- deployment.yml 에 Liveness Probe 옵션 추가
```
livenessProbe:
	tcpSocket:
	  port: 8081
	initialDelaySeconds: 5
	periodSeconds: 5
```
![selfhealing](https://user-images.githubusercontent.com/78134019/109805068-589b1900-7c66-11eb-9565-d44adde4ffc5.jpg)



※ 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW
