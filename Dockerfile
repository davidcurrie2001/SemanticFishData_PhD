FROM stain/jena-fuseki
MAINTAINER Marine Institute
COPY MyOntologyData.rdf /staging/
CMD ["/jena-fuseki/fuseki-server","--file","/staging/MyOntologyData.rdf","/test"]
# docker build -t mi/feas-fuseki:test .
# docker run --name feas-fuseki -d -p 3030:3030 mi/feas-fuseki:test