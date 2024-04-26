package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private SerieRepository repositorio;
    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private List<Serie> series = new ArrayList<>();
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private final String ENDERECO = "https://omdbapi.com/?t=";
    private final String API_KEY = "&apikey=d5591da2";

    public Principal(SerieRepository repositorio){
        this.repositorio = repositorio;
    }

    public void exibeMenu(){

        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                \nDigite a opção que deseja visualizar!
                1 - Buscar Séries
                2 - Buscar Episódios
                3 - Lista de Séries Pesquisadas
                4 - Busca Série por techo do titulo
                5 - Buscar atraves do ator principal
                6 - Top 5 Séries
                7 - Escolha a Categoria
                
                0 - Sair""";

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao){
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 0:
                    System.out.println("Encerrando Aplicação!");
                    break;
                default:
                    System.out.println("Opção Invlálida!");
            }
        }
    }

    private void buscarSerieWeb(){
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da Série para busca!");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();

        System.out.println("De qual Série deseja visualizar os Episódios? ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotaltemporadas(); i++){
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream().map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);

        } else {
            System.out.println("Série não encontrada no nosso Banco de Dados!");
        }
    }

    private void listarSeriesBuscadas() {
        series = repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo(){
        System.out.println("Digite alguma palavra da Série que deseja buscar! ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBuscada.isPresent()){
            System.out.println("A Série que pocura está nesta lista: ");
            System.out.println(serieBuscada);
        } else {
            System.out.println("Não encontramos nenhuma série com este trecho!");
        }
    }

    private void buscarSeriesPorAtor(){
        System.out.println("Digite o nome do Ator principal da Série: ");
        var nomeAtor = leitura.nextLine();

        System.out.println("Digite agora a partir de qual avaliação você deseja ver?");
        var avaliacao = leitura.nextDouble();

        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);

        System.out.println("Aqui estão as Séries que " + nomeAtor + " participa com avaliação acima de !" + avaliacao);
        seriesEncontradas.forEach(s -> System.out.println("Titulo: " + s.getTitulo() + ", Avaliação: " + s.getAvaliacao()));
    }

    private void buscarTop5Series(){
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(s -> System.out.println("Titulo: " + s.getTitulo() + ", Avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Qual categoria ou gênero deseja visualizar?");
        var nomeGenero = leitura.nextLine();

        Categoria categoria = Categoria.fromPortugues(nomeGenero);

        List<Serie> seriesCategoria = repositorio.findByGenero(categoria);
        System.out.println("Aqui estão as Séries do Categoria " + nomeGenero);
        seriesCategoria.forEach(System.out::println);
    }

}