```
every { adder.addOne(any()) } returns -1
every { adder.addOne(3) } answers { callOriginal() }
```


