package io.github.lordraydenmk.superheroesapp.superheroes.superheroslist

import io.github.lordraydenmk.superheroesapp.AppModule
import io.github.lordraydenmk.superheroesapp.R
import io.github.lordraydenmk.superheroesapp.common.ErrorTextRes
import io.github.lordraydenmk.superheroesapp.common.forkAndForget
import io.github.lordraydenmk.superheroesapp.common.parZip
import io.github.lordraydenmk.superheroesapp.common.presentation.ViewModelAlgebra
import io.github.alefgas.superheroesapp.superheroes.NetworkError
import io.github.alefgas.superheroesapp.superheroes.ServerError
import io.github.alefgas.superheroesapp.superheroes.SuperheroException
import io.github.lordraydenmk.superheroesapp.superheroes.data.superheroes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SuperheroesModule : AppModule, ViewModelAlgebra<SuperheroesViewState, SuperheroesEffect>

suspend fun SuperheroesModule.program(actions: Flow<SuperheroesAction>): Unit =
    parZip(Dispatchers.Default, { firstLoad() }, { handleActions(actions) })
    { _, _ -> }

suspend fun SuperheroesModule.handleActions(actions: Flow<SuperheroesAction>) =
    actions.map { handleAction(it) }
        .forkAndForget(Dispatchers.Default, scope)

suspend fun SuperheroesModule.handleAction(action: SuperheroesAction) = when (action) {
    is LoadDetails -> runEffect(NavigateToDetails(action.id))
    Refresh -> loadSuperheroOne()
}

suspend fun SuperheroesModule.firstLoad(): Unit =
    runInitialize { loadSuperheroOne() }

suspend fun SuperheroesModule.loadSuperheroOne(): Unit {
    setState(Loading)
    val state = try {
        val (superheroes, attribution) = superheroes()
        val superheroesVE = superheroes.map { SuperheroViewEntity(it.id, it.name, it.thumbnail) }
        Content(superheroesVE, attribution)
    } catch (t: SuperheroException) {
        mapError(t)
    }
    setState(state)
}

private fun mapError(e: SuperheroException) = when (e.error) {
    is NetworkError -> Problem(ErrorTextRes(R.string.error_recoverable_network))
    is ServerError -> Problem(ErrorTextRes(R.string.error_recoverable_server))
}