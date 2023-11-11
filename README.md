# microservice-demo
##  编译项目
备注：确保maven对应的jdk版本是jdk17及以上，推荐使用maven 3.8.x
``` bash
mvn clean install
```

##  docker启动 nacos 和 mysql
``` bash
docker-compose -f docker-composr-env.yml up --build
```
### nacos地址：<http://localhost:8848/nacos/index.html>

## docker启动微服务
``` bash
docker-compose -f docker-composr-service.yml up --build
```
##  登录网址
<http://127.0.0.1:7573/hello>

## 登录方式
```bash

随意选择一个client

输入用户名：user，密码：123456 登录或者选择github，Google登录
```


# 获取token

* 自定义密码模式

采用网关端口 7573 发起，通过网关将请求转发到认证服务器。

备注：OAuth2.1中，密码模式和简化模式已废弃，需自定义实现

[参考地址]<https://docs.spring.io/spring-authorization-server/docs/current/reference/html/guides/how-to-ext-grant-type.html>

``` bash
curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=authorization_password&username=user&password=123456&scope=profile' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```
* 客户端模式

采用网关端口 7573 发起，通过网关将请求转发到认证服务器。

```bash
curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=client_credentials' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```
* 授权码模式
 备注：授权码模式模式 和 设备授权码模式 涉及浏览器交互，lb://uaa 或者 uaa:9000 获取的 ip 地址与 认证服务器 实际的 ip 地址不一致，需要使用域名或者修改host文件，所以使用认证服务器9000端口

浏览器输入：<http://localhost:9000/oauth2/authorize?response_type=code&client_id=gateway-client-authorization-code&scope=message.read%20message.write%20openid&redirect_uri=http://127.0.0.1:7573/login/oauth2/code/gateway-client-authorization-code>

```bash

登录后获取code替换掉url里的code

curl --location --request POST 'http://localhost:7573/oauth2/token?grant_type=authorization_code&code=yZBLYiRmhI2TBV6284YdBUP-9fQiPY_bXjtI0hi2PcxX3WQN7yuubJC2KAB2YyMnS3SxTJvcrzlLlhyehkyPibWdhlRNnCv-UFLCy5u7Ydd9f4Ntp1XYHmSzhHGki7UM&redirect_uri=http://127.0.0.1:7573/login/oauth2/code/gateway-client-authorization-code' \
--header 'Authorization: Basic Z2F0ZXdheS1jbGllbnQtYXV0aG9yaXphdGlvbi1jb2RlOnNlY3JldA=='
```
* 设备码模式