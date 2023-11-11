# microservice-demo

## 编译项目

备注：确保maven对应的jdk版本是jdk17及以上，推荐使用maven 3.8.x

``` bash
mvn clean install
```

## docker启动 nacos 和 mysql

``` bash
docker-compose -f docker-compose-env.yml up --build
```

### nacos地址：<http://localhost:8848/nacos/index.html>

## docker启动微服务

``` bash
docker-compose -f docker-compose-service.yml up --build
```

## 登录网址

<http://127.0.0.1:7573/hello>

<img src="./img/login-with-oauth2.0.png">

## 登录方式

```bash

随意选择一个client

输入用户名：user，密码：123456 登录或者选择Github，Google登录
```

<img src="./img/login-page.png">



确认授权页面

<img src="./img/consent.jpg">



登录成功页面

<img src="./img/hello-gateway.png">



# 获取token

* 自定义密码模式

采用网关端口 7573 发起，通过网关将请求转发到认证服务器。

备注：OAuth2.1中，密码模式和简化模式已废弃，需自定义实现

参考：[how-to-ext-grant-type.html](https://docs.spring.io/spring-authorization-server/docs/current/reference/html/guides/how-to-ext-grant-type.html)

``` bash
curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=authorization_password&username=user&password=123456&scope=profile' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```

返回值：

```json
{
  "access_token": "eyJraWQiOiJlNzM1NTE4MS0yOWMyLTRlMjktODdkMy02NDg0NzlhYzYwYjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiYXVkIjoiZ2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlIiwibmJmIjoxNjk5NzE0MTc4LCJzY29wZSI6WyJwcm9maWxlIl0sImlzcyI6Imh0dHA6Ly8xNzIuMTguMC41OjkwMDAiLCJleHAiOjE2OTk3MTQ0NzgsImlhdCI6MTY5OTcxNDE3OH0.hhsgREdP40WXQ7FZiIQ8BN4Zzr2ljf5pX9DOzRESTA51WWzm5DcxJziyaBEEM__7P2vzMurBIwSoIMLP_y8OKWJ4hRUh1fSHxv0NBNvhYk7gYU5rF_ZgnXZJ__7keQUnSv9YYSu9j_IoSohMJiW_MDBEICXqCG0TOAXNKAmnI4LYVGVFJ1Sj5LKFOtDmdETf0beJfMejUugEXSXDFXbOpmyd-ZkI094NJ-feKdR-snzv-3662NejtRc8XNRUmvO6uP7XehE7OJsnh858KKI9r3t7civxi5PlINarcl9Hc8c1mAa0mn2-nHcNPhGWUrC4X2EKTmZ8oTzPpk6eLg6EEg",
  "refresh_token": "nSTtMQgjvlFqcTktrPV6Tx1mGMGPXaI45BUAWE87i_1Nrrj7nm8ze9aqZ6LAEejzF4Tu1PjlbmpiBPYkImjw8CoPB49mY-peUaoZCFrP67R56BJ5HuBrOjtv_Qh6R6gA",
  "password": "123456",
  "scope": "profile",
  "token_type": "Bearer",
  "expires_in": 299,
  "username": "user"
}
```

<img src="./img/curl-password.png">

* ldap模式(待完善)

**备注：目前ldap模式校验密码有问题，输入任意密码都可返回token**

``` bash
curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=authorization_ldap&username=user&password=123456&scope=profile' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```

返回值：

```json
{
  "access_token": "eyJraWQiOiJlNzM1NTE4MS0yOWMyLTRlMjktODdkMy02NDg0NzlhYzYwYjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiYXVkIjoiZ2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlIiwibmJmIjoxNjk5NzE0MjYxLCJzY29wZSI6WyJwcm9maWxlIl0sImlzcyI6Imh0dHA6Ly8xNzIuMTguMC41OjkwMDAiLCJleHAiOjE2OTk3MTQ1NjEsImlhdCI6MTY5OTcxNDI2MX0.Kzh7QpyLFeQ9jXKCdobYLQxVbQHwMiBDM2mLuM_icwfnd1QgPBoyOeKp7Gy-73dgVD16rIu3mfwXkA0Z0bBZKv1jRk53WCylnrWkGqZlgn70tRtk6n-J5ft0BD02Hgd47gBcqskNCHG5GgCll9gPKxJDNA5haA1om6O6Ht3Rzh8VK7GLp_bt1yZ0q-ykx0zd5aNEBSsCKnqwJZldPmM46F787pbgdux9BOHgs6qUB4mfrMh3-23VL4MESJeLEwZM7PyXe03IOWrvAaleUYKwBnA_WIPS-XrMzt95pbatGZSFnu4JeP1QvsHf5p9CxSBBnPqwUg3Dykyz6aLEenJOpg",
  "refresh_token": "fAHLiQEwWidWokhbAMvJTmKPQ7ewpRLHxxEXbqslt7Z0wCr5sPCRP9nVIzV5A7AdmaF7eSk498KtuovqGZFWK4UdB2AWbqZpIADpEBF7A_quJ1z5iN8xQeZ4_-Dvhq0T",
  "password": "123456",
  "scope": "profile",
  "token_type": "Bearer",
  "expires_in": 299,
  "username": "user"
}
```

* 客户端模式

采用网关端口 7573 发起，通过网关将请求转发到认证服务器。

```bash
curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=client_credentials' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```
返回值：

```json
{
  "access_token": "eyJraWQiOiJlNzM1NTE4MS0yOWMyLTRlMjktODdkMy02NDg0NzlhYzYwYjQiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJnYXRld2F5LWNsaWVudC1hdXRob3JpemF0aW9uLWNvZGUiLCJhdWQiOiJnYXRld2F5LWNsaWVudC1hdXRob3JpemF0aW9uLWNvZGUiLCJuYmYiOjE2OTk3MTQzMzYsImlzcyI6Imh0dHA6Ly8xNzIuMTguMC41OjkwMDAiLCJleHAiOjE2OTk3MTQ2MzYsImlhdCI6MTY5OTcxNDMzNn0.IqTPdYFo6BzZCZ_KQoAAVLQGRZKZ3nRFsce2LY9cPy96pkb66Co8TLP8VauExNxOPrwPPwnn1BwVKxBoJ6riNvhM-c5D1IxChkKHbmgzDfJpOH8w1KTNO7YompixU-Vy5V1RDEkR6uWZqhko3wIam75zqAJffFtgSK0QvBodVxEL3JgBhafFaisMxPLo5VCWupTScT9I0M372iUxvYkrtAHmhfBAIdxgNJk-KktZco3VWkSUA1CVc_kfqUq5dpc0oDoeDNKkoj4z-TxOhyrhqwifjNGDJ1B6KFZdfQ99kN7RH81LJkLGuR-FdapE4sxRRZmHsGGZK-lEmO6GudkLhQ",
  "token_type": "Bearer",
  "expires_in": 299
}
```

* 授权码模式

备注：授权码模式模式 和 设备授权码模式 均涉及浏览器交互，lb://uaa 或者 使用docker内部网络 uaa:9000 获取的 ip 地址与 认证服务器
实际的 ip 地址不一致，需要使用域名或者修改host文件。故使用认证服务器9000端口

浏览器输入：<http://localhost:9000/oauth2/authorize?response_type=code&client_id=gateway-client-authorization-code&scope=message.read%20message.write%20openid&redirect_uri=http://127.0.0.1:7573/login/oauth2/code/gateway-client-authorization-code>

```bash

登录后获取code替换掉url里的code

curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=authorization_code&code=yZBLYiRmhI2TBV6284YdBUP-9fQiPY_bXjtI0hi2PcxX3WQN7yuubJC2KAB2YyMnS3SxTJvcrzlLlhyehkyPibWdhlRNnCv-UFLCy5u7Ydd9f4Ntp1XYHmSzhHGki7UM&redirect_uri=http://127.0.0.1:7573/login/oauth2/code/gateway-client-authorization-code' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```

* 设备码模式（TODO）

目前使用docker部署后，携带token访问资源服务会报 **The iss claim is not valid**

jwt解密后 payload格式如下

```json
{
  "sub": "user",
  "aud": "gateway-client-authorization-code",
  "nbf": 1699710331,
  "scope": [
    "message.read,message.write"
  ],
  "iss": "http://172.19.0.5:9000",
  "exp": 1699710631,
  "iat": 1699710331
}
```

猜测原因：**实际iss 是: http://uaa实际IP:9000**， 资源服务器配置是：issuer-uri: http://uaa:9000，匹配不上

解决：iss需使用域名

参考：[iss-claim-not-valid-keycloak](https://stackoverflow.com/questions/72854439/iss-claim-not-valid-keycloak)

