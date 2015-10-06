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

    watch: {
      options: {
        livereload: true
      },

      asciidoc: {
        files: ['src/docs/asciidoc/**'],

        tasks: ['shell:asciidoc', 'shell:resources']
      }
    }
  }

  grunt.loadNpmTasks 'grunt-shell'
  grunt.loadNpmTasks 'grunt-contrib-watch'

  grunt.registerTask 'assemble', ['shell:asciidoc']

  grunt.registerTask 'build', ['assemble',
                               'shell:build']

  grunt.registerTask 'default', ['assemble']