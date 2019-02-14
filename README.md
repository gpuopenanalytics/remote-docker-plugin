# Remote Docker Plugin

This a Jenkins plugin for delegating build into a docker container on Jenkins agents.

The plugin also features first-class support for [nvidia-docker](https://github.com/NVIDIA/nvidia-docker) exposing the most common configuration options.

## FAQ

> Why not use [docker-slaves-plugin](https://github.com/jenkinsci/docker-slaves-plugin)?

Two reasons:

1. `docker-slaves-plugin` executes the docker container on either a globally defined docker URL or a job-specific URL. This plugin instead runs the build on a Jenkins agent and the agent executes in a docker container based that node's docker configuration
2. `docker-slaves-plugin` doesn't support modifying the docker runtime (required support `nvidia-docker`)
   
> Do I need to have an NVIDIA GPU on my Jenkins agent to use this plugin?

No, this plugin works without any GPUs. You will be unable to use the `nvidia-docker` runtime and related settings, but builds will still run on agents and execute with docker.
