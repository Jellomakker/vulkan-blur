#version 330

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform MotionBlurConfig {
    float BlendFactor;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 cur = texture(InSampler, texCoord).rgb;
    vec3 prev = texture(PrevSampler, texCoord).rgb;
    // BlendFactor = weight of history (0 = no blur). Exponential frame accumulation.
    fragColor = vec4(mix(cur, prev, BlendFactor), 1.0);
}
