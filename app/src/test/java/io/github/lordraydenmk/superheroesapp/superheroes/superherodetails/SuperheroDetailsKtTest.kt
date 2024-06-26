package io.github.lordraydenmk.superheroesapp.superheroes.superherodetails

import app.cash.turbine.test
import io.github.lordraydenmk.superheroesapp.AppModule
import io.github.lordraydenmk.superheroesapp.R
import io.github.lordraydenmk.superheroesapp.common.ErrorTextRes
import io.github.lordraydenmk.superheroesapp.common.PlaceholderString
import io.github.lordraydenmk.superheroesapp.common.TestViewModel
import io.github.lordraydenmk.superheroesapp.common.presentation.ViewModelAlgebra
import io.github.lordraydenmk.superheroesapp.superheroes.data.ResourceList
import io.github.lordraydenmk.superheroesapp.superheroes.data.SuperheroDto
import io.github.lordraydenmk.superheroesapp.superheroes.data.SuperheroesService
import io.github.lordraydenmk.superheroesapp.superheroes.data.ThumbnailDto
import io.github.lordraydenmk.superheroesapp.superheroes.testSuperheroService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException

class SuperheroDetailsKtTest : FunSpec({

    fun superheroDto(
        id: Long,
        name: String,
        thumbnailPath: String,
        thumbnailExt: String,
        comics: Int = 4,
        stories: Int = 6,
        events: Int = 8,
        series: Int = 2
    ): SuperheroDto =
        SuperheroDto(
            id,
            name,
            ThumbnailDto(thumbnailPath, thumbnailExt),
            ResourceList(comics),
            ResourceList(stories),
            ResourceList(events),
            ResourceList(series)
        )

    fun module(
        service: SuperheroesService,
        viewModelAlgebra: ViewModelAlgebra<SuperheroDetailsViewState, SuperheroDetailsEffect>
    ) = object : SuperheroDetailsModule, AppModule by AppModule.create(service),
        ViewModelAlgebra<SuperheroDetailsViewState, SuperheroDetailsEffect> by viewModelAlgebra {}


    test("FirstLoad - service with success - Superhero") {
        val hulkDto = superheroDto(42, "Hulk", "https://hulk", "jpg", 1, 2, 3, 4)
        val service = testSuperheroService(listOf(hulkDto))

        val viewModel = TestViewModel<SuperheroDetailsViewState, SuperheroDetailsEffect>()
        val module = module(service, viewModel)

        with(module) {
            program(42, emptyFlow())

            val hulk = SuperheroDetailsViewEntity(
                "Hulk",
                "https://hulk.jpg".toHttpUrl(),
                comics = PlaceholderString(R.string.superhero_details_comics, 1),
                stories = PlaceholderString(R.string.superhero_details_stories, 2),
                events = PlaceholderString(R.string.superhero_details_events, 3),
                series = PlaceholderString(R.string.superhero_details_series, 4),
                modified,
                description
            )
            viewModel.viewState.test {
                awaitItem() shouldBe Loading
                awaitItem() shouldBe Content(hulk, "Marvel rocks!")
            }
        }
    }

    test("FirstLoad - service with error - Problem") {
        val error = IOException("Bang")
        val service = testSuperheroService(error)

        val viewModel = TestViewModel<SuperheroDetailsViewState, SuperheroDetailsEffect>()
        val module = module(service, viewModel)

        with(module) {
            program(42, emptyFlow())

            val expectedProblem = Problem(ErrorTextRes(R.string.error_recoverable_network))
            viewModel.viewState.test {
                awaitItem() shouldBe Loading
                awaitItem() shouldBe expectedProblem
            }
        }
    }

    test("Action Up - NavigateUp Effect") {
        val hulkDto = superheroDto(42, "Hulk", "https://hulk", "jpg")
        val viewModel = TestViewModel<SuperheroDetailsViewState, SuperheroDetailsEffect>()
        val module = module(testSuperheroService(listOf(hulkDto)), viewModel)

        with(module) {
            program(42, flowOf(Up))

            viewModel.effects.test {
                awaitItem() shouldBe NavigateUp
            }
        }
    }
})
