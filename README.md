# Exercise generator for `git bisect`

## Prerequisites
* Kotlin 2.0.20 or later

## Usage

```console
$ ./generate-exercise.main.kts [size [seed]]
The first commit is <first-commit-hash>
The problematic commit introduces the string 'htl3EXgz2RG^u5=' in one of the files.
Excercise with seed seed and depth size is ready in <folder>
```

`cd into <folder>` and use `git` to find the commit that introduced the problem.
If you can, solve the problem with two commands.

## References

* [Git bisect](https://git-scm.com/docs/git-bisect)
* [grep man page](https://man7.org/linux/man-pages/man1/grep.1.html)
