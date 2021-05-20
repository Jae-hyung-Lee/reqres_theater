```sh
http http://localhost:8081
HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 20 May 2021 08:49:51 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "profile": {
            "href": "http://localhost:8081/profile"
        },
        "reservations": {
            "href": "http://localhost:8081/reservations{?page,size,sort}",
            "templated": true
        },
        "retrieveMovies": {
            "href": "http://localhost:8081/retrieveMovies"
        },
            "href": "http://localhost:8081/retrieveReservations"
        }
    }
}
```

```sh
http http://localhost:8082
HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 20 May 2021 08:56:24 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "approvals": {
            "href": "http://localhost:8082/approvals{?page,size,sort}",
            "templated": true
        },
        "profile": {
            "href": "http://localhost:8082/profile"
        }
    }
}

```

```sh
http http://localhost:8083
HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 20 May 2021 08:55:41 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "detailinqueries": {
            "href": "http://localhost:8083/detailinqueries"
        },
        "listinqueries": {
            "href": "http://localhost:8083/listinqueries"
        },
        "movieInquiries": {
            "href": "http://localhost:8083/movieInquiries"
        },
        "movieManagements": {
            "href": "http://localhost:8083/movieManagements{?page,size,sort}",
            "templated": true
        },
        "profile": {
            "href": "http://localhost:8083/profile"
        }
    }
}

```

```sh
http http://localhost:8084
HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 20 May 2021 08:56:00 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "movieSeats": {
            "href": "http://localhost:8084/movieSeats{?page,size,sort}",
            "templated": true
        },
        "movies": {
            "href": "http://localhost:8084/movies{?page,size,sort}",
            "templated": true
        },
        "profile": {
            "href": "http://localhost:8084/profile"
        },
        "reservationavailabilityinquiries": {
            "href": "http://localhost:8084/reservationavailabilityinquiries"
        }
    }
}
``