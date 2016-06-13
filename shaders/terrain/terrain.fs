#version 400 core

in vec3 fragColor;
in vec3 surfaceNormal;
in vec3 toLightVector;
in vec4 shadowCoords;
in float visiblity;

out vec4 out_Color;

uniform vec3 lightColor;
uniform mat4 projectionMatrix;

uniform sampler2D shadowMap;

const int pcfCount = 4;
const float totalTexels = (pcfCount * 2.0 + 1.0) * (pcfCount * 2.0 + 1.0);

void main(void){

    float mapSize = 4096.0;
    float texelSize = 1.0 / mapSize;
    float total = 0.0;

    for(int x=-pcfCount; x<=pcfCount; x++){
        for(int y=-pcfCount; y<=pcfCount; y++){
            float objectNearestLight = texture(shadowMap, shadowCoords.xy + vec2(x, y) * texelSize).r;
            if(shadowCoords.z > objectNearestLight + 0.002){
                total += 1.0;
            }
        }
    }

    total /= totalTexels;

    float lightFactor = 1.0 - (total * shadowCoords.w);

    vec3 unitNormal = normalize(surfaceNormal);
    vec3 unitLightVector = normalize(toLightVector);

    float nDotl = dot(unitNormal, unitLightVector);
    float brightness = max(nDotl * lightFactor, 0.4);
    vec3 diffuse = brightness * lightColor;

    out_Color = vec4(fragColor, 1.0) * vec4(diffuse, 1.0);
}