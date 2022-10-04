---
title: "API"
layout: "titled"
nav_order: "a"
parent: "User Docs"
---

{% include toc.md %}

# Introduction

A tree-ware server provides get and set access to a single model (in the future it will support access to multiple
models).

# Get-API

The get-API can be used to get any parts of the model. The parts to get need to be specified in JSON format in the
request body. Since it is not well-defined whether the HTTP GET method supports a request body, the HTTP POST method
must be used for the get-API.

```
POST https://{{host}}/tree-ware/api/get/v1
```

# Set API

The set-API can be used to set any parts of the model. The parts to set need to be specified in JSON format in the
request body. This HTTP POST method must be used for this API as well.

```
POST https://{{host}}/tree-ware/api/get/v1
```

# Versioning

Since the number of APIs is fixed, and since it is the model that evolves, the version is associated with the model and
not the API. However, the version of the model must currently be specified in the API URL (at the end of the URL). In
the future, it may get moved into the model specified in the request body.

The version is a semantic-version and must be specified with a `v` prefix in the URL. Only the major version is
mandatory; the rest of the semantic-version is optional. Typically, only the major version is specified (like in the
examples above).

The server will reject the request if the version specified in the URL is higher than the version of the meta-model
being served.