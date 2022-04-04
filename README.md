
## Description

OpenRefine's extension to add authentication protocols for HTTP/HTTPS request. Compatible with OpenRefine 3.3 supporting: 

* Custom applications with JWT tokens
* RDF extension with basic authentication for AllegroGraph


## Requirements

* Java 1.8.*
* Maven 3.* (mvn)

## Compile

* Execute `mvn package assembly:single`
* Extension will be located into `target/openrefine-authenticator-0.3.zip`

## Installation

* Extract the `openrefine-authenticator-0.3.zip` into the OpenRefine folder `webapp/extensions`
* Run OpenRefine and set up your credentials for all HTTP/HTTPS request


## Run demo

* Add a vhost to your pc
  * Linux examples: `sudo nano /etc/hosts` and add to then end: `127.0.0.1 languages.local.com`
* Move to `demo` folder
* Execute `docker-compose build`
* Run the two containers with `docker-compose up -d`. This will start two containers
    * OpenRefine in http://localhost:3333
    * LoginApp in http://localhost:8888 with two endpoints that you can test
        * ```
          curl --location --request POST 'http://languages.local.com:8080/api/login' \
          --header 'Content-Type: application/json' \
          --data-raw '{
          "username": "admin",
          "password": "password",
          "grant_type": "password"
          }'```
          
        * ```
          curl --location --request GET 'http://localhost:8080/api/languages' \
          --header 'Authorization: Bearer TOKEN_FROM_PREVIOUS_REQUEST''```

* Open in a browser http://127.0.0.1:3333/ then follow the video https://www.loom.com/share/7065806b1fd14e2b83f4412b2829bbd0
* Credentials for http://languages.local.com:8080/api/login:
    * Username: `admin`
    * Password: `password`
    * Type: `Bearer`
* Credentials for http://127.0.0.1:10035/token (AllegroGraph):
    * Username: `admin`
    * Password: `pass`
    * Type: `Basic`