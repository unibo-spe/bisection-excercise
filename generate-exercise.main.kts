#!/usr/bin/env kotlin
// Import Koltin turtle to interact with the shell
@file:DependsOn("com.lordcodes.turtle:turtle:0.10.0")
@file:DependsOn("me.tongfei:progressbar:0.10.2")

import com.lordcodes.turtle.shellRun
import me.tongfei.progressbar.ProgressBar
import java.io.File
import java.io.FileFilter
import kotlin.io.path.createTempDirectory
import kotlin.math.max
import kotlin.random.Random

val problemSize = args.firstOrNull()?.toIntOrNull() ?: 5_000
val progressBar = ProgressBar("Repo creation", problemSize.toLong())
progressBar.setExtraMessage("Preparing...")
val seed = args.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
val random = Random(seed)
val workdir = createTempDirectory().toFile()
println("Creating repo with seed $seed and depth $problemSize in $workdir")
val validChars = ('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()
val separators = listOf('.', '_', '-')
val whitespaces = listOf(' ', '\n')
val baseBranch = "main"

fun randomText(length: Int, vararg additionalChars: List<Char>) = buildString {
    val allChars: List<Char> = validChars + additionalChars.flatMap { it }
    repeat(length) {
        append(allChars.random(random))
    }
}

fun randomFileContent(length: Int) = randomText(
    length,
    listOf(' '..'/', ':'..'@', '['..'`', '{'..'~').flatten() + generateSequence { '\n' }.take(5)
)

fun randomFile() = checkNotNull(workdir.listFiles(FileFilter { it.isFile }) ).randomOrNull(random)

fun commit(message: String = randomText(50, listOf(' '))) = shellRun(workdir) {
    git.addAll()
    git.gitCommand(listOf("commit", "--allow-empty", "-am", message))
}.also { progressBar.extraMessage = message }

fun branches(): List<String> = shellRun(workdir) {
    git.gitCommand(listOf("branch", "--format=%(refname:short)"))
}.lines()

interface Action {
    val likelyhood: Int
    operator fun invoke()
}

val createFile = object : Action {
    override val likelyhood = 15
    override fun invoke() {
        val name = randomText(1) + randomText((0..30).random(random), separators)
        val target = workdir.resolve(name.lowercase())
        if (!target.exists()) {
            target.writeText(randomFileContent((1..20000).random(random)))
            commit("create file $name")
        } else {
            progressBar.extraMessage = "File $name already exists, skipping creation"
        }
    }
}

val editFile = object : Action {
    /*
     * 1. pick a random file
     * 2. count the lines
     * 3. pick a random line group
     * 4. replace the group with a new group of random size
     */
    override val likelyhood: Int = 100

    override fun invoke() {
        val target = randomFile()
        if (target != null) {
            val lines = target.readLines()
            val (start, end) = when {
                lines.size < 2 -> listOf(0, lines.indices.last())
                else -> generateSequence { lines.indices.random(random) }.distinct().take(2).sorted().toList()
            }
            val initialLines = if (start == 0) emptyList() else lines.subList(0, start)
            val finalLines = if (end == lines.size - 1) emptyList() else lines.subList(end + 1, lines.size)
            val replacedLength = lines.subList(start, end + 1).joinToString(separator = "").length
            val maxLength = max(1, replacedLength) * 2
            val newLines = randomFileContent((1..maxLength).random(random))
            target.writeText(
                initialLines.joinToString("\n", postfix = "\n") +
                    newLines +
                    finalLines.joinToString(separator = "\n", prefix = "\n")
            )
            commit("replace lines $start to $end with ${newLines.length} chars in file ${target.name}")
        } else {
            progressBar.extraMessage = "No files to edit, skipping modification"
        }
    }
}

val removeFile = object : Action {
    override val likelyhood = 10

    override fun invoke() {
        if ((workdir.listFiles()?.filter { it.isFile }?.size ?: 0) > 1) {
            val target = randomFile()
            if (target != null) {
                target.delete()
                commit("delete file ${target.name}")
            } else {
                progressBar.extraMessage = "No files to delete, skipping deletion"
            }
        }
    }
}
val branch = object : Action {
    override val likelyhood = 10

    override fun invoke() {
        val branchName = randomText(1) + randomText((5..15).random(random), separators - '.')
        shellRun(workdir) {
            git.checkout(branchName, createIfNecessary = true)
            editFile()
            "edit file in branch $branchName"
        }
    }

}
val merge = object : Action {
    override val likelyhood = 10

    override fun invoke() {
        shellRun(workdir) {
            val branchName = (branches() - git.currentBranch() - baseBranch).randomOrNull(random)
            if (branchName.isNullOrBlank()) {
                "No branches to merge, skipping"
            } else {
                progressBar.extraMessage = "merge $branchName into ${git.currentBranch()}"
                runCatching {
                    git.gitCommand(listOf("merge", "--strategy=ort", "--strategy-option=ours", branchName))
                }.onFailure {
                    progressBar.extraMessage = "merge conflict, forcing resolution"
                    commit("merge conflict resolution")
                }
                progressBar.extraMessage = "delete $branchName"
                git.gitCommand(listOf("branch", "-d", branchName))
            }
        }.also(progressBar::setExtraMessage)
    }
}

val changeBranch = object : Action {
    override val likelyhood: Int = 50

    override fun invoke() {
        val branch = branches().random(random)
        progressBar.extraMessage = "work on branch $branch"
        shellRun(workdir) { git.gitCommand(listOf("checkout", branch)) }
    }
}

val actions = listOf(createFile, editFile, removeFile, branch, merge, changeBranch)
val totalLikelyhood = actions.sumOf { it.likelyhood }

fun selectAction(): Action {
    val selected = random.nextInt(totalLikelyhood)
    var sum = 0
    for (action in actions) {
        sum += action.likelyhood
        if (sum >= selected) {
            return action
        }
    }
    error("No action selected for $selected (max: $totalLikelyhood)")
}

shellRun(workdir) {
    git.gitInit()
    git.checkout(baseBranch, createIfNecessary = true)
    git.gitCommand(listOf("config", "user.email", "unibo-spe@no-reply.github.com"))
    git.gitCommand(listOf("config", "user.name", "Your beloved Software Process Engineering bot"))
    git.gitCommand(listOf("config", "commit.gpgSign", "false"))
    git.gitCommand(listOf("commit", "--allow-empty", "-m", "Initial commit"))
    val firstCommit = git.gitCommand(listOf("rev-parse", "HEAD")).trim()
    createFile()
    createFile()
    createFile()
    progressBar.setExtraMessage("Executing...")
    repeat(problemSize) {
        val action = selectAction()
        action()
        progressBar.step()
    }
    progressBar.setExtraMessage("final merge...")
    git.gitCommand(listOf("checkout", baseBranch))
    while (branches().size > 1) {
        merge()
    }
    println()
    println("The first commit is $firstCommit")
    "done"
}
val hintSize = 10
val candidate = checkNotNull(
    workdir.listFiles { file: File -> file.isFile && file.useLines { lines -> lines.any { it.length > hintSize } } }
        .orEmpty()
        .randomOrNull(random)
) {
    "No files with more than 100 bytes found in ${workdir.absolutePath}, try with another seed."
}
val problemLine: String = candidate.readLines().filter { it.length > hintSize }.random(random)
val start = random.nextInt(problemLine.length - hintSize)
val end = random.nextInt(start + hintSize, problemLine.length)
val problem = problemLine.substring(start..end)
println("The problematic commit introduces the string '$problem' in one of the files.")
println("Excercise with seed $seed and depth $problemSize is ready in $workdir")
