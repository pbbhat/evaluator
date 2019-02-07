# evaluator

To build the distribution run 

``
./gradlew distTar
``

or 

``
./gradlew distZip
``

To create a distribution for local testing run

``
./gradlew installDist
``

You can then test the distribution:

````
cd build/install/evaluator
echo 'https://www.mountainhardwear.com/mens-finder-rain-jacket-1572331.html?cgid=mens-jackets-rain&dwvar_1572331_variationColor=492#start=2' > urls.txt
echo 'https://www.mountainhardwear.com/womens-stretchdown-jacket-1756291.html?cgid=womens-jackets-insulated&dwvar_1756291_variationColor=010#start=1' >> urls.txt
./bin/evaluator urls.txt ../../../src/test/resources/test_template.yaml
````

which should output
````
url title
https://www.mountainhardwear.com/mens-finder-rain-jacket-1572331.html?cgid=mens-jackets-rain&dwvar_1572331_variationColor=492#start=2    Men's Finder™ Rain Jacket 
https://www.mountainhardwear.com/womens-stretchdown-jacket-1756291.html?cgid=womens-jackets-insulated&dwvar_1756291_variationColor=010#start=1   Women's StretchDown™ Jacket 
````