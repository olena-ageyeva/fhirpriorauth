curl -X POST "https://api.availity.com/availity/v1/token"   -H "Content-Type: application/x-www-form-urlencoded"   -d "grant_type=client_credentials&client_id=c82e8ebc0c4ac8d56ea76eaf53c4fa91&client_secret=4c686a28fc5c895548ae74df784565c0&scope=hipaa"

ACCESS_TOKEN=AAIgNjQ2ZmZkZGVlYmYyYjk3NGNjZjAzNjU2YjRjZGJjMTFYJjWd12C_caxslPoWEbNkNPuwI9qyXC_fHzkWM8oDnDpFbEM1knfzCyrZDB-W4VmbeWAlVvi_oR_oGyiHkfQYmild4vRkw4aJSIkaPZPi_udCOymlATvn8MmjZUJo2vc

curl -X POST "https://api.availity.com/availity/v2/service-reviews"  -H "Authorization: Bearer $ACCESS_TOKEN"   -H "Content-Type: application/json"   -H "Accept: application/json"   -H "X-Api-Mock-Scenario-ID: SR-CreateRequestAccepted-i"   -d @src/main/resources/mock-data/mock-service-review.json 

curl -i -X GET "https://api.availity.com/availity/v2/service-reviews/12345678"  -H "Authorization: Bearer $ACCESS_TOKEN"   -H "Accept: application/json"  -H "X-Api-Mock-Scenario-ID: SR-GetComplete-i"
