# Exercise generator for `git bisect`

## Prerequisites
* Kotlin 2.0.20 or later

## Generate an exercise

```console
$ ./generate-exercise.main.kts [size [seed]]
The first commit is <first-commit-hash>docker run --rm -it -v "$(pwd):/workspace" -v /tmp:/tmp --workdir /workspace --entrypoint "/bin/bash" danysk/kotlin "-c" "apt-get update && apt-get install -y git && ./generate-exercise.main.kts"
The problematic commit introduces the string 'htl3EXgz2RG^u5=' in one of the files.
Excercise with seed seed and depth size is ready in <folder>
```

Square brackets mean "optional parameter" (as in classic UNIX Manuals).

* `size` decides how difficult should the exercise be, in number of `git` operations performed on the repo.
Defaults to 5000.
* `seed` is used for reproducibility purposes (same size and seed lead to the same exercise).

### With docker
If you have issues installng Kotlin, you can use docker via:
```
docker run --rm -it -v "$(pwd):/workspace" -v /tmp:/tmp --workdir /workspace --entrypoint "/bin/bash" danysk/kotlin "-c" "apt-get update && apt-get install -y git && ./generate-exercise.main.kts"
```

## Solve the exercise

`cd into <folder>` and use `git` to find the commit that introduced the problem.
If you can, solve the problem with two commands.

## References

* [Git bisect](https://git-scm.com/docs/git-bisect)
* [grep man page](https://man7.org/linux/man-pages/man1/grep.1.html)
