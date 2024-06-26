package io.github.lordraydenmk.superheroesapp.superheroes.data

import io.github.alefgas.superheroesapp.superheroes.NetworkError
import io.github.alefgas.superheroesapp.superheroes.ServerError
import io.github.alefgas.superheroesapp.superheroes.SuperheroException
import io.github.lordraydenmk.superheroesapp.superheroes.domain.Superhero
import io.github.lordraydenmk.superheroesapp.superheroes.domain.SuperheroDetails
import io.github.lordraydenmk.superheroesapp.superheroes.domain.SuperheroId
import io.github.lordraydenmk.superheroesapp.superheroes.domain.Superheroes
import retrofit2.HttpException
import java.io.IOException

suspend fun SuperheroesService.superheroes(): Superheroes {
    val superheroesDto = runRefineError { getSuperheroes() }
    val superheroes = superheroesDto.data.results.map { it.toDomain() }
    return Superheroes(superheroes, superheroesDto.attributionText)
}

suspend fun SuperheroesService.superheroDetails(id: SuperheroId): SuperheroDetails {
    val superheroDto = runRefineError { getSuperheroDetails(id) }
    val superhero = superheroDto.data.results.first().toDomain()
    return SuperheroDetails(superhero, superheroDto.attributionText)
}

private fun SuperheroDto.toDomain(): Superhero = Superhero.create(
    id = id,
    name = name,
    description = description,
    modified = modified,
    thumbnailPath = thumbnail.path,
    thumbnailExt = thumbnail.extension,
    comicsCount = comics.available,
    storiesCount = stories.available,
    eventsCount = events.available,
    seriesCount = series.available
)

private suspend fun <A> runRefineError(f: suspend () -> A): A =
    try {
        f()
    } catch (e: HttpException) {
        throw when (e.code()) {
            in 500..599 -> SuperheroException(
                ServerError(e.code(), e.message())
            )
            else -> throw IllegalStateException("This should NOT happen", e)
        }
    } catch (e: IOException) {
        throw SuperheroException(NetworkError(e))
    }