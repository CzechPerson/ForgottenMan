#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FadeStart;
uniform float FadeEnd;
uniform vec4 FogTint;

in vec4 vertexColor;
in vec2 texCoord0;
in float viewDist;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.5) {
        discard;
    }
    // Vertex color carries vanilla directional face shading; the mesh is baked
    // without AO, so no glitchy near-black corner patches
    vec3 color = tex.rgb * vertexColor.rgb;
    // Reflected copies dissolve into the void with distance.
    float fade = smoothstep(FadeStart, FadeEnd, viewDist);
    color = mix(color, FogTint.rgb, fade);
    fragColor = vec4(color, 1.0) * ColorModulator;
}
