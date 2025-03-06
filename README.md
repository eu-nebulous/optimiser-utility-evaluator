# The NebulOuS utility evaluator module


# Building

To build, a Java development kit version >= 17 must be installed.

```sh
cd utility-evaluator
./mvnw clean install
```

## Building the container

A container can be built with the following commands:

```sh
cd utility-evaluator
docker build -t utility-evaluator -f Dockerfile .
```
