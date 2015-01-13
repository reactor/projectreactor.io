module.exports = (grunt) ->
  grunt.initConfig {
    shell: {
      build: {
        options: {async: true},
        command: './gradlew build'
      },
      resources: {
        options: {async: true},
        command: './gradlew processResources'
      },
      asciidoc: {
        options: {async: true},
        command: './gradlew asciidoctor'
      }
    },

    sass: {
      options: {
        sourcemap: 'none'
      },

      projectreactor: {
        files: {
          'src/main/resources/public/stylesheets/projectreactor.css': 'src/main/sass/projectreactor.scss',
          'src/main/resources/public/stylesheets/docs.css': 'src/main/sass/docs.scss'
        }
      }
    },

    coffee: {
      options: {
        separator: ';',
        sourcemap: false
      },

      projectreactor: {
        files: {
          'src/main/resources/public/javascripts/projectreactor.js': 'src/main/coffee/projectreactor.coffee',
          'src/main/resources/public/javascripts/docs.js': 'src/main/coffee/docs.coffee'
        }
      }
    }

    watch: {
      options: {
        livereload: true
      },

      sass: {
        files: ['src/main/sass/**'],

        tasks: ['sass:projectreactor', 'shell:resources']
      },

      coffee: {
        files: ['src/main/coffee/**'],

        tasks: ['coffee:projectreactor', 'shell:resources']
      },

      asciidoc: {
        files: ['src/docs/asciidoc/**'],

        tasks: ['shell:asciidoc', 'shell:resources']
      }
    }
  }

  grunt.loadNpmTasks 'grunt-shell'
  grunt.loadNpmTasks 'grunt-contrib-sass'
  grunt.loadNpmTasks 'grunt-contrib-coffee'
  grunt.loadNpmTasks 'grunt-contrib-watch'

  grunt.registerTask 'assemble', ['sass:projectreactor',
                                  'coffee:projectreactor',
                                  'shell:asciidoc']

  grunt.registerTask 'build', ['assemble',
                               'shell:build']

  grunt.registerTask 'default', ['assemble']