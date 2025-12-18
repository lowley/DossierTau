
## tracer la création d'un objet si plusieurs instances existantes:

```
private val instanceId = System.identityHashCode(this)  
init {  
    println("instanceId=$instanceId thread = ${Thread.currentThread().name}")  
    println(Throwable("class created here").stackTraceToString())
}
```

