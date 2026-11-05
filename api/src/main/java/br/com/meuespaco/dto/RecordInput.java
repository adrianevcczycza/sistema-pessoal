package br.com.meuespaco.dto;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.constraints.*;
public record RecordInput(@NotNull UUID id,@NotNull @Size(max=80) Map<String,Object> data,@PositiveOrZero long version) {}
